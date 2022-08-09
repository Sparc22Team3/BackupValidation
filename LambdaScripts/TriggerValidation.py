import json
import boto3

region='us-east-1'
instance_id = 'i-0efb6adce411a3707'
ec2 = boto3.resource('ec2', region_name=region)
#Lambda times out after 10 minutes (can increase to 15 max)

def lambda_handler(event, context):
    # Get instance with given id
    instance = ec2.Instance(instance_id)

    #If instance not stopped, stop it, wait till stopped, and start it. 
    if(instance.state['Code'] != 80):
        
        instance.stop()
        
        instance.wait_until_stopped()
        
        instance.start()
        
        instance.wait_until_running()
        
    else:
        #start the instance
        instance.start()
        
        instance.wait_until_running()
        
    return "Current status of {id} instance is {status}" \
    .format(id=instance_id, 
    status=instance.state['Name'])
    