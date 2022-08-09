import json
import boto3

region=''
instance_id = ''
ec2 = boto3.resource('ec2', region_name=region)

#Function timesout at 5 minutes, can increase to 15 max
def lambda_handler(event, context):
    
    instance = ec2.Instance(instance_id)
    
    instance.stop()
        
    instance.wait_until_stopped()
    
    return "Current status of {id} instance is {status}" \
    .format(id=instance_id, 
    status=instance.state['Name'])