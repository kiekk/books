package io.spring.batch.helloworld;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.annotation.OnReadError;
import org.springframework.batch.item.file.FlatFileParseException;

public class CustomerItemListener  {

	private static final Log logger = LogFactory.getLog(CustomerItemListener.class);

	@OnReadError
	public void onReadError(Exception e) {
		if(e instanceof FlatFileParseException) {
			FlatFileParseException ffpe = (FlatFileParseException) e;

			String errorMessage = "An error occured while processing the " +
					ffpe.getLineNumber() +
					" line of the file.  Below was the faulty " +
					"input.\n" +
					ffpe.getInput() + "\n";

			logger.error(errorMessage, ffpe);
		} else {
			logger.error("An error has occurred", e);
		}
	}
}
