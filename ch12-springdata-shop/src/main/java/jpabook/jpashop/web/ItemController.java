package jpabook.jpashop.web;

import jpabook.jpashop.domain.Book;
import jpabook.jpashop.domain.Item;
import jpabook.jpashop.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

@Controller
@RequestMapping(value = "items")
public class ItemController {

    @Autowired
    ItemService itemService;

    @RequestMapping(value = "new", method = RequestMethod.GET)
    public String createForm() {
        return "items/createItemForm";
    }

    @RequestMapping(value = "new", method = RequestMethod.POST)
    public String create(Book item) {
        itemService.saveItem(item);
        return "redirect:/items";
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public String list(Model model) {
        List<Item> items = itemService.findItems();
        model.addAttribute("items", items);
        return "items/itemList";
    }

    @RequestMapping(value = "{itemId}/edit", method = RequestMethod.GET)
    public String updateItemForm(@PathVariable("itemId") Long itemId, Model model) {
        Item item = itemService.findOne(itemId);
        model.addAttribute("item", item);
        return "items/updateItemForm";
    }

    @RequestMapping(value = "{itemId}/edit", method = RequestMethod.POST)
    public String updateItem(@ModelAttribute("item") Book item) {
        itemService.saveItem(item);
        return "redirect:/items";
    }
}
