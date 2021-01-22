package tacos.web;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import tacos.Taco;
import tacos.data.TacoRepository;
import tacos.web.api.TacoResource;
import tacos.web.api.TacoResourceAssembler;

/*
 * 예제와 hateoas 버전이 일치하지 않아서, 관련 메소드, 클래스, 패키지명이 바뀌어서 현재 버전에 맞게 바꿔줘야 합니다.
 * ResourceSupport -> RepresentationModel
 * Resource -> EntityModel
 * Resources -> CollectionModel
 * PagedResources -> PagedModel
 * ResourceAssemblerSupport -> RepresentationModelAssemblerSupport
 * linkTo, methodOn 패키지 변경
 * org.springframework.hateoas.mvc.ControllerLinkBuilder -> org.springframework.hateoas.server.mvc.WebMvcLinkBuilder
 * 
 * 
 */
@RestController
@RequestMapping(path = "/design",produces = {"application/json", "text/html"})
@CrossOrigin(origins = "*")
public class DesignTacoController {
	
	private TacoRepository tacoRepo;
	
	@Autowired
	EntityLinks entityLinks;
	
	public DesignTacoController(TacoRepository tacoRepo) {
		this.tacoRepo = tacoRepo;
	}
	
//	@GetMapping("/recent")
//	public Iterable<Taco> recentTacos(){
//		PageRequest page = PageRequest.of(0, 12, Sort.by("createdAt").descending());
//		return tacoRepo.findAll(page).getContent();
//	}
	
	@GetMapping("/recent")
	public CollectionModel<TacoResource> recentTacos() {
		PageRequest page = PageRequest.of(
				0, 12, Sort.by("createdAt").descending());
		List<Taco> tacos = tacoRepo.findAll(page).getContent();

		CollectionModel<TacoResource> tacoResources = new TacoResourceAssembler().toCollectionModel(tacos);
		CollectionModel<TacoResource> recentResources = new CollectionModel<TacoResource>(tacoResources);
		recentResources.add(
				linkTo(methodOn(DesignTacoController.class).recentTacos())
				.withRel("recents"));
		return recentResources;
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<Taco> tacoById(@PathVariable("id") Long id) {
		Optional<Taco> optTaco = tacoRepo.findById(id);
		
		if(optTaco.isPresent()) {
			return new ResponseEntity<>(optTaco.get(), HttpStatus.OK);
		}
		return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
	}
	
	@PostMapping(consumes = "application/json")
	@ResponseStatus(HttpStatus.CREATED)
	public Taco postTaco(@RequestBody Taco taco) {
		return tacoRepo.save(taco);
	}
	
}
