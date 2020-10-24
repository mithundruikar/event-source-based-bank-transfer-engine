CREATE TABLE TRANSACTION_LOG (
    id NUMBER(10,2) NOT NULL,
    account_id VARCHAR2(20) NOT NULL,
    to_account_id VARCHAR2(20),
    amount NUMBER(10,2),
    direction VARCHAR2(1) CONSTRAINT DIRECTION_CK CHECK (direction in ('', 'D', 'C')),
    transaction_type VARCHAR2(20) NOT NULL,
    create_time TIMESTAMP,
    constraint TRANSACTION_LOG_PK primary key (id)
    --constraint TRANSACTION_LOG_UK unique key (account_id, to_account_id, amount, create_time)
);


CREATE SEQUENCE TRANSACTION_LOG_SEQUENCE
START WITH 1
INCREMENT BY 1
MINVALUE 1
MAXVALUE 10000000
CYCLE;
