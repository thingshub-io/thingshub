package io.thingshub.transport.gb28181;

import gov.nist.javax.sip.parser.MessageParser;
import gov.nist.javax.sip.parser.MessageParserFactory;
import gov.nist.javax.sip.parser.StringMsgParser;
import gov.nist.javax.sip.stack.SIPTransactionStack;

public class Gb28181MessageParserFactory implements MessageParserFactory {

	private static StringMsgParser stringMsgParser = new StringMsgParser();

	public MessageParser createMessageParser(SIPTransactionStack stack) {
		return stringMsgParser;
	}
}