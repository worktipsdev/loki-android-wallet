/*
 * Copyright (c) 2017-2018 m2049r et al.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m2049r.xmrwallet.service.exchange.coingecko;

import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeApi;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeCallback;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeException;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeRate;

import net.jodah.concurrentunit.Waiter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeoutException;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;


public class ExchangeRateTest {

    private MockWebServer mockWebServer;

    private ExchangeApi exchangeApi;

    private final OkHttpClient okHttpClient = new OkHttpClient();
    private Waiter waiter;

    @Mock
    ExchangeCallback mockExchangeCallback;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        waiter = new Waiter();

        MockitoAnnotations.initMocks(this);

        exchangeApi = new ExchangeApiImpl(okHttpClient, mockWebServer.url("/"));
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void queryExchangeRate_shouldBeGetMethod()
            throws InterruptedException {

        exchangeApi.queryExchangeRate(Wallet.LOKI_SYMBOL, "USD", mockExchangeCallback);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("GET", request.getMethod());
    }

    @Test
    public void queryExchangeRate_shouldHavePairInUrl()
            throws InterruptedException {

        exchangeApi.queryExchangeRate(Wallet.LOKI_SYMBOL, "USD", mockExchangeCallback);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/price?ids=loki-network&vs_currencies=usd", request.getPath());
    }

    @Test
    public void queryExchangeRate_wasSuccessfulShouldRespondWithRate()
            throws TimeoutException {
        final String base = Wallet.LOKI_SYMBOL;
        final String quote = "EUR";
        final double rate = 1.56;
        MockResponse jsonMockResponse = new MockResponse().setBody(
                createMockExchangeRateResponse("loki-network", quote, rate));
        mockWebServer.enqueue(jsonMockResponse);

        exchangeApi.queryExchangeRate(base, quote, new ExchangeCallback() {
            @Override
            public void onSuccess(final ExchangeRate exchangeRate) {
                waiter.assertEquals(exchangeRate.getBaseCurrency(), base);
                waiter.assertEquals(exchangeRate.getQuoteCurrency(), quote);
                waiter.assertEquals(exchangeRate.getRate(), rate);
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.fail(e);
                waiter.resume();
            }
        });
        waiter.await();
    }

    @Test
    public void queryExchangeRate_wasSuccessfulShouldRespondWithRateUSD()
            throws TimeoutException {
        final String base = Wallet.LOKI_SYMBOL;
        final String quote = "USD";
        final double rate = 1.56;
        MockResponse jsonMockResponse = new MockResponse().setBody(
                createMockExchangeRateResponse("loki-network", quote, rate));
        mockWebServer.enqueue(jsonMockResponse);

        exchangeApi.queryExchangeRate(base, quote, new ExchangeCallback() {
            @Override
            public void onSuccess(final ExchangeRate exchangeRate) {
                waiter.assertEquals(exchangeRate.getBaseCurrency(), base);
                waiter.assertEquals(exchangeRate.getQuoteCurrency(), quote);
                waiter.assertEquals(exchangeRate.getRate(), rate);
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.fail(e);
                waiter.resume();
            }
        });
        waiter.await();
    }

    @Test
    public void queryExchangeRate_wasNotSuccessfulShouldCallOnError()
            throws TimeoutException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        exchangeApi.queryExchangeRate(Wallet.LOKI_SYMBOL, "USD", new ExchangeCallback() {
            @Override
            public void onSuccess(final ExchangeRate exchangeRate) {
                waiter.fail();
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.assertTrue(e instanceof ExchangeException);
                waiter.assertTrue(((ExchangeException) e).getCode() == 500);
                waiter.resume();
            }

        });
        waiter.await();
    }

    @Test
    public void queryExchangeRate_unknownAssetShouldCallOnError()
            throws TimeoutException {
        MockResponse jsonMockResponse = new MockResponse().setBody(
                createMockExchangeRateErrorResponse());
        mockWebServer.enqueue(jsonMockResponse);

        exchangeApi.queryExchangeRate(Wallet.LOKI_SYMBOL, "ABC", new ExchangeCallback() {
            @Override
            public void onSuccess(final ExchangeRate exchangeRate) {
                waiter.fail();
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.assertTrue(e instanceof ExchangeException);
                ExchangeException ex = (ExchangeException) e;
                waiter.assertTrue(ex.getCode() == 200);
                waiter.assertEquals(ex.getErrorMsg(), "No price found");
                waiter.resume();
            }

        });
        waiter.await();
    }

    private String createMockExchangeRateResponse(final String base, final String quote, final double rate) {
        /*
        {
          "loki-network": {
            "usd": 0.166536
          }
        }
        */
        return "{" +
                    "\"" + base + "\": {" +
                        "\"" + quote.toLowerCase() + "\":" + rate +
                    "}" +
                "}";
    }

    private String createMockExchangeRateErrorResponse() {
        return "{}";
    }
}
