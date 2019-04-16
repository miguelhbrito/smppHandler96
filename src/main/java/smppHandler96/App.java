package smppHandler96;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.commons.util.windowing.WindowFuture;
import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.LoggingOptions;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppBindException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.cloudhopper.smpp.util.PduUtil;

import cliente.Client;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
	public static Logger log = LoggerFactory.getLogger(App.class);

	private static void log(WindowFuture<Integer, PduRequest, PduResponse> future) {
		SubmitSm req = (SubmitSm) future.getRequest();
		SubmitSmResp resp = (SubmitSmResp) future.getResponse();

		log.debug("Resposta recebida com MSG ID={} para APPID={}", resp.getMessageId(), req.getReferenceObject());
	}

	public static void main(String[] args) throws SmppInvalidArgumentException {
		SmppSessionConfiguration sessionCfg = new SmppSessionConfiguration();

		sessionCfg.setType(SmppBindType.TRANSCEIVER);
		sessionCfg.setHost("127.0.0.1");
		sessionCfg.setPort(2775);
		sessionCfg.setSystemId("smppclient1");
		sessionCfg.setPassword("password");

		LoggingOptions loggingOpt = new LoggingOptions();
//		loggingOpt.setLogPdu(false);
//		loggingOpt.setLogBytes(false);
		sessionCfg.setLoggingOptions(loggingOpt);
		Client client = new Client(sessionCfg);
		client.setSessionHandler(new MySmppSessionHandler(client));
		ExecutorService pool = Executors.newFixedThreadPool(2);
		pool.submit(client);
		client.start();

		log.debug("Esperando conectar !!");

		while (client.getSession() == null || !client.getSession().isBound()) {

			if (client.getSession() != null) {
				log.debug("Sessao é {}", client.getSession().isBound());
			} else {
				log.debug("Sessao null !!");
			}
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException ex) {
				java.util.logging.Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		log.debug("Tentando mandar !!!!");
		
		SubmitSm sm = new SubmitSm();
		sm.setSourceAddress(new Address((byte) 5, (byte) 0, "Teste"));
		sm.setDestAddress(new Address((byte) 1, (byte) 1, "123456789"));
		sm.setShortMessage(CharsetUtil.encode("Testando 9.6!", "UTF-8"));
		sm.setRegisteredDelivery((byte) 1);
		sm.setDataCoding((byte) 8);

		try {
			client.getSession().submit(sm, TimeUnit.SECONDS.toMillis(60));
		} catch (RecoverablePduException ex) {
			java.util.logging.Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
		} catch (UnrecoverablePduException ex) {
			java.util.logging.Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
		} catch (SmppTimeoutException ex) {
			java.util.logging.Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
		} catch (SmppChannelException ex) {
			java.util.logging.Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
		} catch (InterruptedException ex) {
			java.util.logging.Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
		}
		try {
			TimeUnit.SECONDS.sleep(30);
		} catch (InterruptedException ex) {
			java.util.logging.Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
		}

		client.stop();
		pool.shutdown();
	}
}
