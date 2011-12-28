package net.frontlinesms.ui.i18n;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Currency;
import org.apache.log4j.Logger;
import net.frontlinesms.FrontlineUtils;

/**
 * Class for formatting floating point numbers into currency strings. TODO
 * should use {@link BigDecimal} or something instead of floating point numbers.
 * 
 * @author Alex Anderson | Gonçalo Silva
 */
public class CurrencyFormatter {
	//> INSTANCE VARIABLES
	private final Logger log = FrontlineUtils.getLogger(this.getClass());
	private final NumberFormat currencyFormat;
	
	/** Create a new {@link CurrencyFormatter} */
	public CurrencyFormatter(String currencyFormat) {
		if(currencyFormat.contains("#") | currencyFormat.contains("0")){
			this.currencyFormat = new DecimalFormat(currencyFormat);
		} else {
			// assume supplied code is a currency code.  If it doesn't parse, use the platform default
			NumberFormat nf = NumberFormat.getCurrencyInstance();
			
			try {
				Currency currency = Currency.getInstance(currencyFormat);
				nf.setCurrency(currency);
			} catch (Exception ex) {
				log.info("Could not set currency using supplied code '" + currencyFormat + "'; will use default.");
			}
			
			this.currencyFormat = nf;
		}
	}

	/**
	 * Format a floating point number into a string representation of a currency
	 * value.
	 * @param input number
	 * @return formatted currency string
	 */
	public String format(double input) {
		return currencyFormat.format(input);
	}

	/**
	 * Format a {@link BigDecimal} number into a string representation of a currency
	 * value.
	 * @param input number
	 * @return formatted currency string
	 */
	public String format(BigDecimal input) {
		return currencyFormat.format(input);
	}
}
