package lv.janis.iom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class IomApplication {

	public static void main(String[] args) {
		SpringApplication.run(IomApplication.class, args);
	}

}
