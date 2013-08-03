from com.twilio.sdk.verbs import TwiMLResponse

#
# Simple test workflow
#
def execute(req):
    print req
    yield TwiMLResponse()
