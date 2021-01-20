package tacos;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.CreditCardNumber;

import lombok.Data;

@Data
@Entity
@Table(name = "Taco_Order")
public class Order {
	
	//day02
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private Date placedAt;
	
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
	
	@ManyToMany(targetEntity = Taco.class)
	private List<Taco> tacos = new ArrayList<>();
	
	public void addDesign(Taco design) {
		this.tacos.add(design);
	}
	
	@PrePersist
	void placedAt() {
		this.placedAt = new Date();
	}
	
	@ManyToOne
	private User user;
}
