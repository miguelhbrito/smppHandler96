package cliente;

import com.cloudhopper.smpp.SmppClient;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client implements Runnable{
	public static final Logger log = LoggerFactory.getLogger(Client.class);

	protected SmppSessionConfiguration cfg;
	protected SmppSessionHandler sessionHandler;
	protected ClientState clientState;
	protected volatile SmppSession session;
	protected SmppClient smppClient;
	protected ScheduledExecutorService timer;
	protected ScheduledFuture<?> elinkTask;
	protected ScheduledFuture<?> rebindTask;
	protected long rebindPeriod = 5;
	protected long elinkPeriod = 5;
	
	public Client(SmppSessionConfiguration cfg) {
		this.cfg = cfg;
		this.timer = Executors.newScheduledThreadPool(2);
	}
	
	public void run() {
		log.debug("Criando cliente");

		this.clientState = ClientState.IDLE;

		while (this.clientState != ClientState.STOPPING) {
			try {
				TimeUnit.MILLISECONDS.sleep(10);
			} catch (InterruptedException ex) {
				/* */
			}
		}

		this.smppClient.destroy();
		this.clientState = ClientState.STOPPED;		
	}
	
	public void start() {
		log.debug("Startando client !!");
		this.smppClient = new DefaultSmppClient();
		this.bind();
	}

	public void bind() {
		if (
			this.clientState == ClientState.BOUND || this.clientState == ClientState.IDLE
		) {
			log.debug("Bindando !!!");

			if (
				this.session != null && this.session.isBound()
			) {
				this.session.close();
				this.session.destroy();
			}

			this.clientState = ClientState.BINDING;

			if (elinkTask != null) {
				this.elinkTask.cancel(true);
			}
			runRebindTask();
		}
	}
	
	private void runRebindTask() {
		this.rebindTask = this.timer.scheduleAtFixedRate(new RebindTask(this), 0, getRebindPeriod(), TimeUnit.SECONDS);
	}

	private void runElinkTask() {
		this.elinkTask = this.timer.scheduleAtFixedRate(new ElinkTask(this), getElinkPeriod(), getElinkPeriod(), TimeUnit.SECONDS);
	}
	
	public void bound(SmppSession session) {
		if (this.clientState == ClientState.BINDING) {
			log.debug("Bound state");

			this.clientState = ClientState.BOUND;
			this.session = session;

			if (rebindTask!=null) {
				this.rebindTask.cancel(true);
			}
			runElinkTask();
		}
	}

	public void stop() {
		log.debug("Stopping !!");
		
		this.clientState = ClientState.STOPPING;
		this.elinkTask.cancel(true);
		this.rebindTask.cancel(true);
		this.timer.shutdown();
		this.timer = null;
	}

	public SmppSessionConfiguration getCfg() {
		return cfg;
	}

	public void setCfg(SmppSessionConfiguration cfg) {
		this.cfg = cfg;
	}

	public SmppSessionHandler getSessionHandler() {
		return sessionHandler;
	}

	public void setSessionHandler(SmppSessionHandler sessionHandler) {
		this.sessionHandler = sessionHandler;
	}

	public SmppSession getSession() {
		return session;
	}

	public void setSession(SmppSession session) {
		this.session = session;
	}

	public SmppClient getSmppClient() {
		return smppClient;
	}

	public void setSmppClient(SmppClient smppClient) {
		this.smppClient = smppClient;
	}

	public long getRebindPeriod() {
		return rebindPeriod;
	}

	public void setRebindPeriod(long rebindPeriod) {
		this.rebindPeriod = rebindPeriod;
	}

	public long getElinkPeriod() {
		return elinkPeriod;
	}

	public void setElinkPeriod(long elinkPeriod) {
		this.elinkPeriod = elinkPeriod;
	}



}
