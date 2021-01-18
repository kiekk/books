package tacos;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.CreditCardNumber;

import lombok.Data;

@Data
public class Order {
	
	@NotBlank(message = "Name is required")
	private String deliveryName;
	
	@NotBlank(message = "Street is required")
	private String deliveryStreet;
	
	@NotBlank(message = "City is required")
	private String deliveryCity;
	
	@NotBlank(message = "State is required")
	private String deliveryState;
	
	@NotBlank(message = "Zip code is required")
	private String deliveryZip;
	
	//신용카드 유효성 검사를 Hibernate에서 제공하는 @CreditCardNumber 어노테이션으로 
	//간단하게 검사할 수 있습니다.
	@CreditCardNumber(message = "Not a valid credit card number")
	private String ccNumber;
	
	@Pattern(regexp = "^(0[1-9]|1[0-2])([\\\\/])([1-9][0-9])$",
			message = "Must be formatted MM/YY")
	private String ccExpiration;
	
	//integer -> 자리수 결정
	//fraction -> 소수점 자리수 결정
	@Digits(integer = 3, fraction = 0, message = "Invalid CVV")
	private String ccCVV;
}
