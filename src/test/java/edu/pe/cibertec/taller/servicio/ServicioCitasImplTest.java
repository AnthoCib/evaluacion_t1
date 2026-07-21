package edu.pe.cibertec.taller.servicio;

import edu.pe.cibertec.taller.excepcion.CitaNoCancelableException;
import edu.pe.cibertec.taller.excepcion.EspecialidadIncorrectaException;
import edu.pe.cibertec.taller.excepcion.HorarioNoPermitidoException;
import edu.pe.cibertec.taller.excepcion.MecanicoNoEncontradoException;
import edu.pe.cibertec.taller.modelo.*;
import edu.pe.cibertec.taller.repositorio.RepositorioCitas;
import edu.pe.cibertec.taller.repositorio.RepositorioMecanicos;
import edu.pe.cibertec.taller.servicio.impl.ServicioCitasImpl;
import edu.pe.cibertec.taller.util.ProveedorFechaHora;
import edu.pe.cibertec.taller.util.ServicioNotificaciones;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioCitasImplTest {

	@Mock
	private RepositorioMecanicos repositorioMecanicos;

	@Mock
	private RepositorioCitas repositorioCitas;

	@Mock
	private ProveedorFechaHora proveedorFechaHora;

	@Mock
	private ServicioNotificaciones servicioNotificaciones;

	private ServicioCitasImpl servicioCitas;

	private String nombreMecanico;
	private String placa;
	private LocalDateTime ahora;
	private LocalDateTime fechaCita;

	private Mecanico mecanicoCambioAceite;
	private Mecanico mecanicoReparacionMotor;
	@BeforeEach
	void inicializar() {
		servicioCitas = new ServicioCitasImpl(repositorioMecanicos, repositorioCitas,
				proveedorFechaHora, servicioNotificaciones);
		// TODO: crear aqui los datos comunes que necesiten los tests

		nombreMecanico = "Anthony Cordero";

		placa = "COR-859";

		ahora = LocalDateTime.of(
				2026, 9, 18, 8, 0
		);

		fechaCita = LocalDateTime.of(
				2026, 9, 19, 10, 0
		);

		mecanicoCambioAceite = new Mecanico(
				1L,
				nombreMecanico,
				TipoServicio.CAMBIO_ACEITE
		);

		mecanicoReparacionMotor = new Mecanico(
				2L,
				nombreMecanico,
				TipoServicio.REPARACION_MOTOR
		);
	}

	@Test
	@DisplayName("P01:Agendar una cita valida al guardar, notifica y la retorna en estado PROGRAMADA")
	void agendarCitaExitosa() {
		// Arrange
		// TODO
		when(proveedorFechaHora.ahora()).thenReturn(ahora);
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanicoCambioAceite));
		when(repositorioCitas.findByMecanicoIdAndEstado(1L, EstadoCita.PROGRAMADA))
				.thenReturn(Collections.emptyList());
		when(repositorioCitas.save(any(Cita.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		// Act
		// TODO
			Cita cita = servicioCitas.agendarCita(1L,placa,TipoServicio.CAMBIO_ACEITE,fechaCita);


		// Assert
		// TODO: verificar estado, duracion, save y notificacion

		assertEquals(EstadoCita.PROGRAMADA,cita.getEstado());
		assertEquals(1,cita.getDuracionHoras());
		assertEquals(nombreMecanico,cita.getMecanico().getNombre());
		verify(repositorioCitas).save(cita);
		verify(servicioNotificaciones).notificarCitaAgendada(cita);
	}

	@Test
	@DisplayName("P01:Agendar con un mecanico inexistente lanza MecanicoNoEncontradoException")
	void agendarConMecanicoInexistente() {
		// Arrange
		// TODO

		long idInexistente = 99L;

		when(repositorioMecanicos.findById(idInexistente))
				.thenReturn(Optional.empty());

		// Act y Assert
		// TODO
		MecanicoNoEncontradoException excepcion = assertThrows(
				MecanicoNoEncontradoException.class,
				() -> servicioCitas.agendarCita(
						idInexistente,placa,TipoServicio.CAMBIO_ACEITE,fechaCita
				));

		assertTrue(excepcion.getMessage().contains("99"));
		verify(repositorioCitas,never()).save(any(Cita.class));
	}

	@Test
	@DisplayName("P01:Agendar cuando la especialidad no coincide lanza EspecialidadIncorrectaException")
	void agendarConEspecialidadIncorrecta() {
		// Arrange
		// TODO
		when(repositorioMecanicos.findById(2L))
				.thenReturn(Optional.of(mecanicoCambioAceite));

		// Act y Assert
		// TODO

		EspecialidadIncorrectaException exception = assertThrows(
				EspecialidadIncorrectaException.class,
				() -> servicioCitas.agendarCita(
						2L,placa,TipoServicio.REPARACION_MOTOR,fechaCita
				));

		assertTrue(exception.getMessage().contains("REPARACION_MOTOR"));
	}



	@Test
	@DisplayName("P02:Una reparacion de motor a las 07:00 se rechaza")
	void agendarServicioPesadoALasSiete() {
		// Arrange

		LocalDateTime fechaInicio = LocalDateTime.of(
				2026, 9, 19, 7, 0
		);

		when(repositorioMecanicos.findById(2L))
				.thenReturn(Optional.of(mecanicoReparacionMotor));

		// Act
		HorarioNoPermitidoException excepcion = assertThrows(
				HorarioNoPermitidoException.class,
				() -> servicioCitas.agendarCita(
						2L,
						placa,
						TipoServicio.REPARACION_MOTOR,
						fechaInicio
				)
		);

		// Assert
		assertTrue(excepcion.getMessage().contains("08:00"));
		verify(repositorioCitas, never()).save(any(Cita.class));
	}

	@Test
	@DisplayName("P02:Una reparacion de motor a las 08:00 se acepta y se guarda")
	void agendarServicioPesadoEnLaManana() {
		// Arrange

		LocalDateTime fechaInicio = LocalDateTime.of(
				2026, 9, 19, 8, 0
		);

		when(proveedorFechaHora.ahora())
				.thenReturn(ahora);

		when(repositorioMecanicos.findById(2L))
				.thenReturn(Optional.of(mecanicoReparacionMotor));

		when(repositorioCitas.findByMecanicoIdAndEstado(
				2L,
				EstadoCita.PROGRAMADA
		)).thenReturn(Collections.emptyList());

		when(repositorioCitas.save(any(Cita.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		Cita cita = servicioCitas.agendarCita(
				2L,
				placa,
				TipoServicio.REPARACION_MOTOR,
				fechaInicio
		);

		// Assert
		assertEquals(EstadoCita.PROGRAMADA, cita.getEstado());
		assertEquals(4, cita.getDuracionHoras());
		assertEquals(fechaInicio, cita.getFechaHoraInicio());

		verify(repositorioCitas).save(cita);
		verify(servicioNotificaciones).notificarCitaAgendada(cita);
	}

	@Test
	@DisplayName("P02:Una reparacion de motor a las 11:00 se acepta y se guarda")
	void agendarServicioPesadoALasOnce() {
		// Arrange


		LocalDateTime fechaInicio = LocalDateTime.of(
				2026, 9, 19, 11, 0
		);

		when(proveedorFechaHora.ahora())
				.thenReturn(ahora);

		when(repositorioMecanicos.findById(2L))
				.thenReturn(Optional.of(mecanicoReparacionMotor));

		when(repositorioCitas.findByMecanicoIdAndEstado(
				2L,
				EstadoCita.PROGRAMADA
		)).thenReturn(Collections.emptyList());

		when(repositorioCitas.save(any(Cita.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		Cita cita = servicioCitas.agendarCita(
				2L,
				placa,
				TipoServicio.REPARACION_MOTOR,
				fechaInicio
		);

		// Assert
		assertEquals(EstadoCita.PROGRAMADA, cita.getEstado());
		assertEquals(4, cita.getDuracionHoras());
		assertEquals(fechaInicio, cita.getFechaHoraInicio());

		verify(repositorioCitas).save(cita);
		verify(servicioNotificaciones).notificarCitaAgendada(cita);
	}
	@Test
	@DisplayName("P02:Una reparacion de motor a las 12:00 se rechaza")
	void agendarServicioPesadoEnLaTarde() {
		// Arrange


		LocalDateTime fechaInicio = LocalDateTime.of(
				2026, 9, 19, 12, 0
		);

		when(repositorioMecanicos.findById(2L))
				.thenReturn(Optional.of(mecanicoReparacionMotor));
		// Act
		HorarioNoPermitidoException excepcion = assertThrows(
				HorarioNoPermitidoException.class,
				() -> servicioCitas.agendarCita(
						2L,
						placa,
						TipoServicio.REPARACION_MOTOR,
						fechaInicio
				)
		);

		// Assert
		assertTrue(excepcion.getMessage().contains("12:00"));
		verify(repositorioCitas, never()).save(any(Cita.class));
	}


	@DisplayName("Agendar en una fecha del pasado lanza FechaInvalidaException")
	void agendarConFechaEnElPasado() {
		// Arrange
		// TODO: recuerden mockear proveedorFechaHora.ahora()

		// Act y Assert
		// TODO
	}


	@DisplayName("Agendar sobre una cita ya programada se rechaza con HorarioOcupadoException")
	void agendarConSuperposicion() {
		// Arrange
		// TODO

		// Act y Assert
		// TODO
	}


	@DisplayName("Una cita que empieza justo cuando termina otra se acepta")
	void agendarCitaContigua() {
		// Arrange
		// TODO: una cita existente que termina a las 10:00 y la nueva que empieza a las 10:00

		// Act
		// TODO

		// Assert
		// TODO
	}

	@Test
	@DisplayName("Cancelar con 24 horas o mas de anticipacion no genera penalidad")
	void cancelarConAnticipacionSuficiente() {

		// Arrange
		// TODO

		long idCita = 10L;

		LocalDateTime horaCancelacion = LocalDateTime.of(
				2026, 9, 18, 10, 0
		);

		Cita cita = new Cita(
				idCita,
				mecanicoCambioAceite,
				placa,
				TipoServicio.CAMBIO_ACEITE,
				fechaCita,
				1,
				EstadoCita.PROGRAMADA
		);

		when(proveedorFechaHora.ahora())
				.thenReturn(horaCancelacion);

		when(repositorioCitas.findById(idCita))
				.thenReturn(Optional.of(cita));

		when(repositorioCitas.save(cita))
				.thenReturn(cita);

		// Act
		// TODO
		ResultadoCancelacion resultado =
				servicioCitas.cancelarCita(idCita);

		// Assert
		// TODO: penalidad 0, estado CANCELADA, notificacion
		assertEquals(0.0, resultado.getMontoPenalidad());
		assertEquals(EstadoCita.CANCELADA, cita.getEstado());
		assertTrue(resultado.isExitoso());
		assertEquals(nombreMecanico, cita.getMecanico().getNombre());

		verify(repositorioCitas).save(cita);
		verify(servicioNotificaciones)
				.notificarCitaCancelada(cita);
	}

	@Test
	@DisplayName("Cancelar faltando 2 horas aplica una penalidad de 50.00")
	void cancelarConAvisoTardio() {
		// Arrange
		long idCita = 11L;

		LocalDateTime horaCancelacion = LocalDateTime.of(
				2026, 9, 19, 8, 0
		);

		Cita cita = new Cita(
				idCita,
				mecanicoCambioAceite,
				placa,
				TipoServicio.CAMBIO_ACEITE,
				fechaCita,
				1,
				EstadoCita.PROGRAMADA
		);

		when(proveedorFechaHora.ahora())
				.thenReturn(horaCancelacion);

		when(repositorioCitas.findById(idCita))
				.thenReturn(Optional.of(cita));

		when(repositorioCitas.save(cita))
				.thenReturn(cita);

		// Act
		ResultadoCancelacion resultado =
				servicioCitas.cancelarCita(idCita);

		// Assert
		assertEquals(50.0, resultado.getMontoPenalidad());
		assertEquals(EstadoCita.CANCELADA, cita.getEstado());

		verify(repositorioCitas).save(cita);
		verify(servicioNotificaciones)
				.notificarCitaCancelada(cita);
	}


	@DisplayName("Cancelar una cita inexistente lanza CitaNoEncontradaException")
	void cancelarCitaInexistente() {
		// Arrange
		// TODO

		// Act y Assert
		// TODO
	}

	@Test
	@DisplayName("Cancelar una cita que ya fue atendida lanza CitaNoCancelableException")
	void cancelarCitaYaAtendida() {
		// Arrange
		long idCita = 12L;

		Cita cita = new Cita(
				idCita,
				mecanicoCambioAceite,
				placa,
				TipoServicio.CAMBIO_ACEITE,
				fechaCita,
				1,
				EstadoCita.ATENDIDA
		);

		when(repositorioCitas.findById(idCita))
				.thenReturn(Optional.of(cita));

		// Act
		CitaNoCancelableException excepcion = assertThrows(
				CitaNoCancelableException.class,
				() -> servicioCitas.cancelarCita(idCita)
		);

		// Assert
		assertTrue(excepcion.getMessage().contains("programadas"));
		assertEquals(nombreMecanico, cita.getMecanico().getNombre());

		verify(repositorioCitas, never())
				.save(any(Cita.class));

		verify(servicioNotificaciones, never())
				.notificarCitaCancelada(any(Cita.class));
	}



	@DisplayName("Buscar mecanico disponible retorna el primero sin citas superpuestas")
	void buscarMecanicoDisponibleRetornaPrimeroLibre() {
		// Arrange
		// TODO: dos mecanicos de la misma especialidad, el primero ocupado

		// Act
		// TODO

		// Assert
		// TODO
	}


	@DisplayName("Buscar mecanico cuando ninguno esta libre lanza SinDisponibilidadException")
	void buscarMecanicoSinDisponibilidad() {
		// Arrange
		// TODO

		// Act y Assert
		// TODO
	}
}
