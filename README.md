## **MSSQL log based CDC - streaming to java**

*@master 브랜치로 가면 Source가 있습니다.*  
*해당 코드는 DML 캐치하는 수준의 코드입니다. 많이 부족하지만 감안해서 봐주세요.*

---

**트랜잭션 로그를 읽고 Schema_Table_CT 테이블에 변경 데이터를 캡처합니다.**

MSSQL의 CDC는 데이터베이스의 변경을 추적하고 해당 변경 사항을 대상으로 하는 작업을 수행하기 위해 로그 파일을 사용합니다. CDC 작업은 CDC 데이터베이스를 구ㅎ성하여 변경 데이터를 저장하고 이를 추적하는 CDC추적을 만듭니다.

트랜잭션이 실행되면, CDC 추적이 로그 파일의 변경 사항을 읽어들이고, 이를 추적 테이블(CT Table)이라는 특별한 형태의 테이블에 저장합니다. 해당 테이블에 저장된 변경 데이터는 다른 데이터베이스나 애플리케이션으로 복제할 수 있습니다.

> 장점

* 실시간 데이터 캡처 (근접 실시간)   
* 자동 로그 관리    
    + 자동으로 로그 파일을 관리하며, 별도의 로그 백업 작업 필요X  
* 데이터 무결성 보장하나 주의가 필요  
    + 여러 테이블간의 변경 사항 발생 시, 트랜잭션 단위로 캡처하므로 데이터 무결성을 보장  
    + 그러나, 트랜잭션의 커밋 로그를 기반으로 하기에 커밋되지 않은 트랜잭션은 캡처 X  
    + 또한, DDL과 스키마 변경사항 기록 X    

> 단점

* 오버헤드 발생   
    + 로그 파일을 추적하고, 변경 내용을 캡처하고, 추적 테이블을 유지-관리하는 등의 작업 필요  
* 일부 버전 제한    
    + MSSQL 2012, 2014, 2016, 2017, 2019 Enterprise Edition 등에서 사용 가능 (그 이전은 생략)  

---

### 1. local settings

* MSSQL 2019 Developer Edition (64-bit) on Linux (Ubuntu 20.04.5 LTS)
* Intellij
* Java 8

### 1. MSSQL docker container 

 
```
version: '3.7'
services:
  sqlserver:
    image: mcr.microsoft.com/mssql/server:2019-latest
    user: root
    container_name: sql1
    hostname: sql1
    ports:
      - 1433:1433
    volumes:
      - ./volume/sql1data:/var/opt/mssql
    environment:
      ACCEPT_EULA: Y
      SA_PASSWORD: password1!
```

* SQL Server Agent 를 사용하도록 설정 (기본적으로 사용하지 않도록 설정되어있음)

```
-- (컨테이너 생성 이후, 내부에서 실행)
$ su -
$ /opt/mssql/bin/mssql-conf set sqlagent.enabled true

-- (restart 도커 컨테이너)
```


### 2. CDC 활성화

```
-- 1. CDC 활성화 (Database 범위에서의 활성화)
EXECUTE sys.sp_cdc_enable_db;

-- 2. CDC 대상 테이블 설정 (Table 단위에서의 활성화)
EXEC sys.sp_cdc_enable_table  
@source_schema = N'<스키마>',  
@source_name   = N'<테이블명>',  
@role_name     = NULL,  
@supports_net_changes = 1;
```

### 추가

특정 사용자 계정에서 진행 시, sysadmin이나 db_owner 권한 필요

```
-- Create CDC user
use master;
CREATE LOGIN new_user WITH PASSWORD = 'password1!';
CREATE USER new_user FOR LOGIN new_user;

-- Grant permissions to CDC user
GRANT CREATE PROCEDURE TO new_user;
GRANT EXECUTE ON sys.sp_cdc_enable_db TO new_user;
GRANT CREATE TABLE TO new_user;
GRANT EXECUTE ON sys.sp_cdc_enable_table TO new_user;

use [데이터베이스명];
EXEC sys.sp_cdc_enable_db;

-- sysadmin 권한 부여 및 db_owner 권한 부여
USE master;

ALTER SERVER ROLE sysadmin ADD MEMBER [new_user];
use [데이터베이스명];

USE master;
EXEC sp_addrolemember 'db_owner', 'new_user';
```

