from com.twilio.sdk.verbs import TwiMLResponse
from org.droste.ivr import LastTwilioResponse

#
# Simple test workflow
#
def execute(req):
    print req
    yield TwiMLResponse()
