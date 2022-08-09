# BackupValidator

## Running BackupValidator on an EC2 Instance
1. Setup the EC2 Instance.
    - This instance must be able to connect via SSH using a private key to restored versions of the EC2 instance that was backed up.
    - This instance must be able to connect to the restore RDS instance.
    - Security Groups can be used to ensure access to the other resources.
1. Install Java 11
1. Install Chrome (Used in Selenium Tests)
    - Amazon Linux 2 [Installing Google Chrome on Centos](https://intoli.com/blog/installing-google-chrome-on-centos/)
1. Setup Role for BackupValidator
    - See [BackupValidatorPolicy](docs/BackupValidatorPolicy.json) for minimum actions BackupValidator needs.  The policy also needs to allow those actions on the resources that BackupValidator creates when it restores the backups.
1. Upload BackupValidator.jar to instance.
1. Run BackupValidator with --newconfig to setup configuration file
1. Run BackupValidator with --newselenium to setup selenium tests
1. Run BackupValidator
    - Check the options available with --help

## Running BackupValidator on Local Machine
1. Setup your AWS credentials on your machine: [Set up AWS Credentials and Region for Development](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html)
    - Your credentials must have at least permissions in [BackupValidatorPolicy](docs/BackupValidatorPolicy.json)
    - You must be able to connect via SSH using a private key to the restored versions of the EC2 instance that was backed up.
    - You must be able to connect to the restored RDS instance via SSH tunnels.
        - You will be provided with a prompt after the RDS instance has been restored with the command to run to setup the tunnel to the newly created RDS instance and the production RDS instance and 2 minutes to create the tunnels from another terminal. At that point you will just need the public DNS name of an EC2 instance with access to the RDS instances and the path to the private key file that allows logging into that EC2 instance.
        - `ssh -i "Path\to\PrivateKeyFile" -N -l ec2-user -L 3306:[Production RDS Instance Endpoint Address]:3306 -L 3307:[Restored RDS Instance Endpoint Address- Must wait until instance is restored to get this]:3306  [EC2 Public Address With Access to Database With SSH Access From This Machine] -v`
1. Install Java 11
1. Install Chrome (Used in Selenium Testing)
1. Run BackupValidator with --newconfig to setup configuration file
1. Run BackupValidator with --newselenium to setup selenium tests
1. Run BackupValidator
    - Check the options available with --help
    
## Accessing Reports
Reports will be saved in BackupValidationReports in the same directory you run BackupValidator in.

You can also setup an SNS Topic and provide that topic's ARN to the configuration settings of BackupValidator to send the report to any endpoints you wish the report to go to.

### Report
- Report will list each recovery point restored and if it passed the tests.
- Report will include whether the web app as a whole has passed the Selenium tests after connecting the different resources.
- If there are any tests that have failed, the specifics of the failed tasks will be included after the summary of recovery points that passed.

## Logs
By default logs will be saved in the same directory that BackupValidator has been run from. They will be rotated by day.  The logs will include information about status of the tests at any given point and is generally the same as the output to the command line.

## Config File
The default location for the config file is in the user's home directory in `.config/BackupValidator`. You can use `--newconfig` to setup a new config file or `--modifyconfig` to modify an existing config file.  You can also provide your own config.json file, [Sample Config File](/docs/SampleConfig.json).

## Selenium File
The default location for the Selenium tests file is in the user's home directory in `.config/BackupValidator`.  You can use `--newselenium` to setup a new Selenium file or `--modifyselenium` to modify an existing Selenium file.  You can also provide your own selenium.json file, [Sample Selenium File](/docs/SampleSelenium.json).

## Running BackupValidator on a Schedule: Lambda Functions

Configure BackupValidator to run at startup with a cron job. Use `crontab -e` to add a cron job.  This will open a file for editing, to the end of this file add `reboot java -jar BackupValidator.jar` assuming you have placed BackupValidator.jar in the user's home directory.  You still must setup the configuration and Selenium files either manually or through BackupValidator's `--newconfig` and `--newselenium` options.

The [lambda scripts](/LambdaScripts) start and stop an instance identified by a region, id pair. Users must customize the [TriggerValidation](/LambdaScripts/TriggerValidation.py) and [StopValidationInstance](/LambdaScripts/StopValidationInstance.py) scripts with the appropriate id and region of the instance responsible for executing BackupValidator.  Along with these scripts, users should attach a 24-hour trigger ('rate(1 day)') to the TriggerValidation script using EventBridge. An existing SNS topic to which BackupValidator publishes should be used as a trigger for the StopValidationInstance script. Users can also attach an SNS topic as a destination to each lambda script to ensure that the instance starts and stops appropriately. 

Since the scripts will wait on the instance until it reaches a start or stopped state, the timeout of each lambda implementation should be increased to five minutes or as suitable based on the use case. Users must also give the appropriate permissions to each lambda script. Suggested policies for [SNS Functionality](/docs/LambdaSNSPolicy.json) and [starting and stopping the validation instance](/docs/LambdaValidationPolicy.json) have been provided. 
