
DROP TABLE QUEUE;
CREATE cached TABLE QUEUE (
	   ID integer generated by default as identity (start with 1)
     , que_name VARCHAR(128)
     , element OBJECT
);

