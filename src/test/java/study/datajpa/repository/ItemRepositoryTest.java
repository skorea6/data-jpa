package study.datajpa.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import study.datajpa.entity.Item;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ItemRepositoryTest {

    @Autowired
    ItemRepository itemRepository;

    @Autowired
    EntityManager em;

    @Test
    public void save() throws Exception {
        //given
        Item item = new Item("abc");
        System.out.println("1: " + item.getId()); // null

        // save 에는 이미 transactional 이 있다. (Spring data jpa)
        itemRepository.save(item);

        System.out.println("2: " + item.getId()); // 1

        //when

        //then
    }
}