package es.unizar.tmdad.lab2.configuration;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.social.twitter.api.StreamListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.twitter.api.Tweet;
import es.unizar.tmdad.lab2.domain.MyTweet;
import es.unizar.tmdad.lab2.domain.TargetedTweet;
import es.unizar.tmdad.lab2.service.TwitterLookupService;

@Configuration
@EnableIntegration
@IntegrationComponentScan
@ComponentScan
public class TwitterFlow {
	
	@Autowired
	 private TwitterLookupService lookupService;

	@Bean
	public DirectChannel requestChannel() {
		return new DirectChannel();
	}

	
	@Bean
	public IntegrationFlow sendTweet() {
		return IntegrationFlows.from(requestChannel())
				//handle("streamSendingService", "sendTweet").get();
				//filtro para asegurar que se trata de un Tweet
								.filter((Object o) -> o instanceof Tweet)
								//transformación del Tweet en un TargetedTweet con los tópicos coincidentes
								.<Tweet,TargetedTweet>transform(t -> new TargetedTweet(new MyTweet(t),
										lookupService.getQueries().stream().filter(topic -> t.getUnmodifiedText().contains(topic)).collect(Collectors.toList())))
								//división del TargetedTweet con varios tópicos, en varios TargetedTweet con un sólo tópico
								.split(TargetedTweet.class, tt -> {
									ArrayList<TargetedTweet> listTargetedTweets = new ArrayList<TargetedTweet>();
									for(String s : tt.getTargets()) {
										listTargetedTweets.add(new TargetedTweet(tt.getTweet(), s));
									}
									return listTargetedTweets;
								})
								//transformación del texto del TargetedTweet para mostrar en negrita las palabras que coinciden con el tópico buscado
								.<TargetedTweet,TargetedTweet>transform(tt -> {
									tt.getTweet().setUnmodifiedText(tt.getTweet().getUnmodifiedText().replaceAll(tt.getFirstTarget(), "<b>" + tt.getFirstTarget() + "</b>"));
									return tt;
								})
								.handle("streamSendingService", "sendTweet")
								.get();
				
				
	}
	
	
	
	
	

	
	
	
}

@MessagingGateway(name = "integrationStreamListener", defaultRequestChannel = "requestChannel")
interface MyStreamListener extends StreamListener {

}

