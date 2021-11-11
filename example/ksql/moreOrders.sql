CREATE OR REPLACE  TABLE MOREORDERS AS
SELECT STRUCT(TransactionMainId := TransactionId, TransactionMainDate := TransactionMainDate) as TX, COLLECT_LIST(Number)[1] as Number, COLLECT_LIST(Date)[1] as Date, COLLECT_LIST(State)[1] as State
FROM  ORDERSSTREAM
GROUP BY STRUCT(TransactionMainId := TransactionId, TransactionMainDate := TransactionMainDate)
EMIT CHANGES;
