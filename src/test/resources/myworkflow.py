from com.twilio.sdk.verbs import TwiMLResponse
from org.twilioworkflow import LastTwilioResponse

#
# Simple test workflow
#
def execute(req):
    print req
    yield TwiMLResponse()
