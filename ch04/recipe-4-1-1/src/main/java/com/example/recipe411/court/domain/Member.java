package com.example.recipe411.court.domain;

import javax.xml.bind.annotation.XmlRootElement;


// @XmlRootElement : 객체 필드를 자동 감치해 XML 데이터로 변환
// ex) name=join -> <name>join</name>
@XmlRootElement
public class Member {

    private String name;
    private String phone;
    private String email;

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
