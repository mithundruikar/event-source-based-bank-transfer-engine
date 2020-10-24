insert into TRANSACTION_LOG (id, account_id, to_account_id, amount, direction, transaction_type, create_time)
values (TRANSACTION_LOG_SEQUENCE.nextval, 'ABC', 'DEF', 100, 'D', 'PENDING_DEBIT', sysdate-3);

insert into TRANSACTION_LOG (id, account_id, to_account_id, amount, direction, transaction_type, create_time)
values (TRANSACTION_LOG_SEQUENCE.nextval, 'DEF', 'ABC', 100, 'C', 'PENDING_CREDIT', sysdate-3);

insert into TRANSACTION_LOG (id, account_id, to_account_id, amount, direction, transaction_type, create_time)
values (TRANSACTION_LOG_SEQUENCE.nextval, 'ABC', 'ABC', 200, 'C', 'TOPUP', sysdate-2);

insert into TRANSACTION_LOG (id, account_id, to_account_id, amount, direction, transaction_type, create_time)
values (TRANSACTION_LOG_SEQUENCE.nextval, 'ABC', 'DEF', 100, 'D', 'CNF_PAYBACK_DEBIT', sysdate-1);

insert into TRANSACTION_LOG (id, account_id, to_account_id, amount, direction, transaction_type, create_time)
values (TRANSACTION_LOG_SEQUENCE.nextval, 'DEF', 'ABC', 100, 'C', 'CNF_PAYBACK_CREDIT', sysdate-1);

insert into TRANSACTION_LOG (id, account_id, to_account_id, amount, direction, transaction_type, create_time)
values (TRANSACTION_LOG_SEQUENCE.nextval, 'ABC', 'XYZ', 100, 'D', 'CNF_DEBIT', sysdate);

insert into TRANSACTION_LOG (id, account_id, to_account_id, amount, direction, transaction_type, create_time)
values (TRANSACTION_LOG_SEQUENCE.nextval, 'XYZ', 'ABC', 100, 'C', 'CNF_CREDIT', sysdate);

insert into TRANSACTION_LOG (id, account_id, to_account_id, amount, direction, transaction_type, create_time)
values (TRANSACTION_LOG_SEQUENCE.nextval, 'ABC', 'XYZ', 200, 'D', 'PENDING_DEBIT', sysdate);

insert into TRANSACTION_LOG (id, account_id, to_account_id, amount, direction, transaction_type, create_time)
values (TRANSACTION_LOG_SEQUENCE.nextval, 'XYZ', 'ABC', 200, 'C', 'PENDING_CREDIT', sysdate);

