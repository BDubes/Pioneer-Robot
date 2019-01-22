# Robot Controller Software

This controller software makes use of the ARIA library to move a Pioneer robot connected over serial. To be used in conjunction with the [Robot Controller Interface](https://github.com/Christopher-Dreiser/Robot-Controller-Interface).

## Deployment

In order to use this controller, an auth.json file must be created in the base directory with the following:
```
{
  "queue": "[NAME_OF_QUEUE]",
  "URI": "amqp://[USERNAME]:[PASSWORD]@[HOST_URL]"
}
```
The queue and URI specified must be the same as the one in the [Robot Controller Interface](https://github.com/Christopher-Dreiser/Robot-Controller-Interface) you wish to control the robot with. 
