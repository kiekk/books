begin;

drop table IF EXISTS batch_job_execution cascade ;
drop table IF EXISTS batch_job_execution_context cascade;
drop table IF EXISTS batch_job_instance cascade;
drop table IF EXISTS batch_job_params cascade;
drop table IF EXISTS batch_step_execution cascade;
drop table IF EXISTS batch_step_execution_context cascade ;

DROP SEQUENCE IF EXISTS batch_job_execution_seq;
DROP SEQUENCE IF EXISTS batch_job_seq;
DROP SEQUENCE IF EXISTS batch_step_execution_seq;

commit; 