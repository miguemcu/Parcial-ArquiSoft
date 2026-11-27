package com.udea.parcial_equipo_4;

import org.springframework.boot.SpringApplication;

public class TestParcialEquipo4Application {

	public static void main(String[] args) {
		SpringApplication.from(ParcialEquipo4Application::main).with(TestcontainersConfiguration.class).run(args);
	}

}
