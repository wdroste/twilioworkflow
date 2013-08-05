Twiliow Workflow
==============

This is a server to simplify the use of the the Twilio Service. The basic concept is to take an asynchronous process and make it appear synchronous. Instead of handling various files of PHP or XML on the server there's just one process file written Python. The Python engine retains the state during each HTTP request.

The example from Twilio's web site for a basic IVR system. http://www.twilio.com/docs/howto/ivrs-the-basics Uses a PHP file per descision. Below is the sample re-written in one Python file. 

Sample: src/main/webapp/WEB-INF/sample.py


