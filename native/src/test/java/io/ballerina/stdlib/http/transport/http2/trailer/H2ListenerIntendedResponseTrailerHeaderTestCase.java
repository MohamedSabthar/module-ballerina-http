/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package io.ballerina.stdlib.http.transport.http2.trailer;

import io.ballerina.stdlib.http.transport.contentaware.listeners.TrailerHeaderListener;
import io.ballerina.stdlib.http.transport.contract.HttpClientConnector;
import io.ballerina.stdlib.http.transport.contract.config.ListenerConfiguration;
import io.ballerina.stdlib.http.transport.contract.config.SenderConfiguration;
import io.ballerina.stdlib.http.transport.contract.config.TransportsConfiguration;
import io.ballerina.stdlib.http.transport.contract.exceptions.ServerConnectorException;
import io.ballerina.stdlib.http.transport.contractimpl.DefaultHttpWsConnectorFactory;
import io.ballerina.stdlib.http.transport.message.HttpCarbonMessage;
import io.ballerina.stdlib.http.transport.message.HttpConnectorUtil;
import io.ballerina.stdlib.http.transport.message.HttpMessageDataStreamer;
import io.ballerina.stdlib.http.transport.trailer.TrailerHeaderTestTemplate;
import io.ballerina.stdlib.http.transport.util.Http2Util;
import io.ballerina.stdlib.http.transport.util.TestUtil;
import io.ballerina.stdlib.http.transport.util.client.http2.MessageGenerator;
import io.ballerina.stdlib.http.transport.util.client.http2.MessageSender;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Test case for H2 trailer headers come along with inbound intended response.
 *
 * @since 6.3.0
 */
public class H2ListenerIntendedResponseTrailerHeaderTestCase extends TrailerHeaderTestTemplate {
    private HttpClientConnector h2ClientWithPriorKnowledge;

    @BeforeClass
    public void setup() {
        ListenerConfiguration listenerConfiguration = Http2Util.getH2CListenerConfiguration();
        HttpHeaders trailers = new DefaultLastHttpContent().trailingHeaders();
        trailers.add("foo", "bar");
        trailers.add("baz", "sabtharm");
        trailers.add("Max-forwards", "five");
        super.setup(listenerConfiguration, trailers, TrailerHeaderListener.MessageType.RESPONSE);

        TransportsConfiguration transportsConfiguration = new TransportsConfiguration();
        SenderConfiguration senderConfiguration = Http2Util.getH2CSenderConfiguration();
        h2ClientWithPriorKnowledge = new DefaultHttpWsConnectorFactory().createHttpClientConnector(
                HttpConnectorUtil.getTransportProperties(transportsConfiguration), senderConfiguration);
    }

    @Test
    public void testNoPayload() {
        String testValue = "";
        HttpCarbonMessage httpMsg = MessageGenerator.generateRequest(HttpMethod.GET, testValue);
        verifyResult(httpMsg, h2ClientWithPriorKnowledge, testValue);
    }

    @Test
    public void testSmallPayload() {
        String testValue = "Test Http2 Message";
        HttpCarbonMessage httpMsg = MessageGenerator.generateRequest(HttpMethod.POST, testValue);
        verifyResult(httpMsg, h2ClientWithPriorKnowledge, testValue);
    }

    @Test
    public void testLargePayload() {
        String testValue = TestUtil.largeEntity;
        HttpCarbonMessage httpMsg = MessageGenerator.generateRequest(HttpMethod.POST, testValue);
        verifyResult(httpMsg, h2ClientWithPriorKnowledge, testValue);
    }

    private void verifyResult(HttpCarbonMessage httpCarbonMessage, HttpClientConnector http2ClientConnector,
                              String expectedValue) {
        HttpCarbonMessage response = new MessageSender(http2ClientConnector).sendMessage(httpCarbonMessage);
        assertNotNull(response);
        String result = TestUtil.getStringFromInputStream(new HttpMessageDataStreamer(response).getInputStream());
        assertEquals(result, expectedValue, "Expected response not received");
        assertEquals(response.getHeaders().get("Trailer"), "foo, baz, Max-forwards");
        assertEquals(response.getTrailerHeaders().get("foo"), "bar");
        assertEquals(response.getTrailerHeaders().get("baz"), "sabtharm");
        assertEquals(response.getTrailerHeaders().get("Max-forwards"), "five");
    }

    @AfterClass
    public void cleanUp() throws ServerConnectorException {
        super.cleanUp();
    }
}
