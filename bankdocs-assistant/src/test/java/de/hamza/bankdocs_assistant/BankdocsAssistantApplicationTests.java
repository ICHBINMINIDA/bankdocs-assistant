package de.hamza.bankdocs_assistant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "rag.ingest-on-startup=false")
class BankdocsAssistantApplicationTests {

	@Test
	void contextLoads() {
	}

}
