package tacos.web.api;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import tacos.Taco;
import tacos.data.TacoRepository;

@RepositoryRestController
public class RecentTacosController {
	
	private TacoRepository tacoRepo;
	
	public RecentTacosController(TacoRepository tacoRepo) {
		this.tacoRepo = tacoRepo;
	}
	
	@GetMapping(path = "/tacos/recent", produces = "application/hal+json")
	public ResponseEntity<CollectionModel<TacoResource>> recentTacos(){
		PageRequest page = PageRequest.of(0, 12, Sort.by("createdAt").descending());
		
		List<Taco> tacos = tacoRepo.findAll(page).getContent();
		
		List<TacoResource> tacoResources = (List<TacoResource>) new TacoResourceAssembler().toCollectionModel(tacos);
		
		CollectionModel<TacoResource> recentResources = new CollectionModel<TacoResource>(tacoResources);
		
		recentResources.add(linkTo(methodOn(RecentTacosController.class).recentTacos())
						.withRel("recents"));
		return new ResponseEntity<CollectionModel<TacoResource>>(recentResources, HttpStatus.OK);
						
	}
}
