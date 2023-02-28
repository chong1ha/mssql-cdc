package com.example.mssqlcdc;

import com.example.mssqlcdc.config.CDCOperationType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author gunha
 * @version 0.1
 * @since 2023-02-28 오후 5:23
 */
@Log4j2
@Component
public class CDCStreamingRunner implements ApplicationRunner {

    /* CDC 스키마의 해당 테이블에 접근해 변경 데이터를 지속적으로 가져오도록 하는 쿼리문 */
    private static final String CDC_QUERY = "SELECT * FROM cdc.fn_cdc_get_all_changes_dbo_cdc_table (?, ?, 'all')";
    /* CDC 컬럼명 가져오기 */
    private static final String COLUMN_QUERY = "SELECT column_name FROM cdc.captured_columns;";
    private static final String START_LSN = "SELECT CONVERT(BIGINT, sys.fn_cdc_get_min_lsn('dbo_cdc_table'))";
    private static final String END_LSN = "SELECT CONVERT(BIGINT, sys.fn_cdc_get_max_lsn())";


    private final JdbcTemplate jdbcTemplate;
    @Autowired
    public CDCStreamingRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        // 최초 실행시 CDC 테이블의 시작 LSN 값을 가져옴
        Long startLsn = jdbcTemplate.queryForObject(START_LSN, Long.class);
        // 컬럼명 조회
        List<String> columnNames = jdbcTemplate.queryForList(COLUMN_QUERY, String.class);

        while (true) {
            // 최신 LSN 값을 가져와서 쿼리 파라미터로 설정
            Long endLsn = jdbcTemplate.queryForObject(END_LSN, Long.class);
            if (endLsn > startLsn) { // 최신 데이터만 가져오기

                // CDC 변경 데이터 가져오기
                List<Map<String, Object>> results = jdbcTemplate.queryForList(CDC_QUERY, startLsn, endLsn);
                for (Map<String, Object> row : results) {
                    /**
                     * [CDC 테이블 컬럼]
                     *   => __$start_lsn: CDC 데이터의 시작 LSN 값
                     *   => __$end_lsn: CDC 데이터의 끝 LSN 값
                     *   => __$seqval: CDC 데이터의 순차값 (변경 데이터가 생성될 때마다 1씩 증가)
                     *   => __$operation: CDC 데이터의 변경된 작업의 유형(INSERT, UPDATE, DELETE)
                     *   => __$update_mask: CDC 데이터의 변경된 컬럼의 마스크 정보 (이진 값)
                     *   => __$command_id: 데이터 변경 작업을 유일하게 실별하는 값
                     *                     변경 작업이 특정 트랜잭션 내에서 여러 번 실행될 경우, 동일한 값이 할당됨 (데이터 무결성 검증용 값)
                     *
                     *  [세부]
                     *  1. lsn 은 16진수 형태의 바이너리 값으로, 데이터베이스에는 ASCII 문자로 인코딩한 결과값이 들어가져 있음
                     *  2. update_mask 는 CDC 변경 행의 어떤 열이 수정되었는 지를 나타내는 정보로, 특정 열을 수정하지 않았을 경우에는 값이 비어있음
                     *     만약 변경 행에서 특정 열만 수정했을 경우, 비어있지 않고 수정된 열의 비트맵 정보가 저장됨
                     *     하지만, 변경 행에서 모든 열을 수정했을 경우, 값은 비어있을 수 있음
                     */
                    System.out.println(row);

                    // [세부 1] Operation 값 출력
                    int operationCode = (int) row.get("__$operation");
                    CDCOperationType operation = CDCOperationType.values()[operationCode - 1];
                    System.out.println("--> Operation = "+ operation);

                    // [세부 2] seqval 값 출력
                    byte[] seqvalBytes = (byte[]) row.get("__$seqval");
                    String seqvalHex = Hex.encodeHexString(seqvalBytes);
                    System.out.println("--> Seqval = " + seqvalHex);

                    // [세부 3] update mask 값 출력
                    byte[] updateMaskBytes = (byte[]) row.get("__$update_mask");
                    for (int i = 0; i < updateMaskBytes.length * 8; i++) {
                        if (((updateMaskBytes[i / 8] >> (i % 8)) & 0x01) == 0x01) {
                            System.out.println("--> Column[" + i + "] "+ columnNames.get(i) + " has been updated.");
                        }
                    }
                }
                // 변경 데이터 처리 후, 최신 LSN 값으로 startLsn 갱신
                startLsn = endLsn;
            }
            // 1초마다 변경 데이터 확인
            Thread.sleep(1000);
        }
    }
}
