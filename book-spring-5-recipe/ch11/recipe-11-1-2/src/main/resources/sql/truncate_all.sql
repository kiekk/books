-- this script is meant to truncate the Spring Batch-specific tables
begin ; 
truncate IF EXISTS batch_job_execution cascade ;
truncate IF EXISTS batch_job_execution_context cascade;
truncate IF EXISTS batch_job_instance cascade;
truncate IF EXISTS batch_job_params cascade;
truncate IF EXISTS batch_step_execution cascade;
truncate IF EXISTS batch_step_execution_context cascade ;
commit; 
 