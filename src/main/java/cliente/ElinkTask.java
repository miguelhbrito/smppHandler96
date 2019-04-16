package cliente;

import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import java.util.concurrent.TimeUnit;
import org.slf4j.LoggerFactory;

public class ElinkTask implements Runnable{
	
	public static final org.slf4j.Logger log = LoggerFactory.getLogger(ElinkTask.class);
	protected Client client;
	public ElinkTask(Client client) {
		this.client = client;
	}
	
	public void run() {
		if (client.clientState == ClientState.BOUND) {
			SmppSession session = client.getSession();

			log.debug("Enviando elink");

			try {
				session.enquireLink(new EnquireLink(), TimeUnit.SECONDS.toMillis(10));

				log.debug("Elink enviado com sucesso !!");
			} catch (RecoverablePduException ex) {
				log.debug("{}", ex);
			} catch (UnrecoverablePduException ex) {
				log.debug("{}", ex);
			} catch (SmppTimeoutException ex) {
				client.bind();

				log.debug("{}", ex);
			} catch (SmppChannelException ex) {
				log.debug("{}", ex);
			} catch (InterruptedException ex) {
				log.debug("{}", ex);
			}
		}		
	}

}
