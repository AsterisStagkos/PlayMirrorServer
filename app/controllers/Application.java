package controllers;

import java.net.URISyntaxException;
import java.sql.SQLException;

import models.Pinger;
import play.libs.Akka;
import play.libs.F.Callback0;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import scala.concurrent.duration.Duration;
import views.html.index;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;


public class Application extends Controller {
    public static WebSocket<byte[]> pingWs() {
    //	System.out.println("message received: " + message );
        return new WebSocket<byte[]>() {
            public void onReady(WebSocket.In<byte[]> in, WebSocket.Out<byte[]> out) {
                final ActorRef pingActor = Akka.system().actorOf(Props.create(Pinger.class, in, out));
                final Cancellable cancellable = Akka.system().scheduler().schedule(Duration.create(1, "seconds"),
                                                   Duration.create(1, "seconds"),
                                                   pingActor,
                                                   "Tick",
                                                   Akka.system().dispatcher(),
                                                   null
                                                   ); 
                
                in.onClose(new Callback0() {
                	@Override
                	public void invoke() throws Throwable {
                		cancellable.cancel();
                	}
                });
                
             //   out.write("hey");
                
            //  	final ActorRef actor = Akka.system().actorOf(Props.create(Pinger.class, in, out));
     
            	
            } 

        };
    }

    public static Result pingJs() throws SQLException, URISyntaxException {
    //	 Database table creates, only to be done once
    	//	DatabaseHandler.enterUpdateStatement("DROP TABLE ExperimentParticipation");
		//	DatabaseHandler.enterUpdateStatement("CREATE TABLE Users (id int NOT NULL, name varchar(255), androidid int, PRIMARY KEY (id))");
	//		DatabaseHandler.enterUpdateStatement("CREATE TABLE Experiments (id int NOT NULL, name varchar(255), creator varchar(255), description varchar(255), filepath varchar(255), independent boolean, PRIMARY KEY (id))");
	//		DatabaseHandler.enterUpdateStatement("CREATE TABLE ExP (userid varchar(255) NOT NULL, experimentidOne varchar(255))");

		
        return ok(views.js.ping.render());
    }
    

    public static Result index() {
        return ok(index.render());
    }
   
}
