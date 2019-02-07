 --New job status values: PENDING AND WAITING
  
ALTER TYPE fstep_jobs_status ADD VALUE 'PENDING' AFTER 'CANCELLED';

ALTER TYPE fstep_jobs_status ADD VALUE 'WAITING' AFTER 'PENDING';


