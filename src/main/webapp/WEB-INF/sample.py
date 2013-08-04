from com.twilio.sdk.verbs import TwiMLResponse
from com.twilio.sdk.verbs import Gather
from com.twilio.sdk.verbs import Say
from com.twilio.sdk.verbs import Redirect
from org.droste.ivr import LastTwilioResponse


# Sample workflow from Twilio
def execute(req):

    # Greeting
    gather = Gather()
    gather.setNumDigits(1)
    gather.append(Say("Welcome to TPS."))
    gather.append(Say("For store hours, press 1."))
    gather.append(Say("To speak to an agent, press 2."))
    gather.append(Say("To check your package status, press 3."))

    resp = TwiMLResponse()
    resp.append(gather)
    resp.append(Say("Sorry, I didn't get your response."))

    # send response, wait for reply..
    req = yield resp

    resp = LastTwilioResponse()
    digit = int(req.Digits)
    if 1 == digit:
        resp.append(Say("Our store hours are 8 AM to 8 PM everyday."))
    elif 2 == digit:
        resp.append(Say("Let me connect you to an agent."))
    else:
        print "Invalid"

    yield resp
