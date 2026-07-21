package edu.pe.cibertec.taller.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import edu.pe.cibertec.taller.excepcion.HorarioOcupadoException;
import edu.pe.cibertec.taller.modelo.Cita;
import edu.pe.cibertec.taller.modelo.EstadoCita;
import edu.pe.cibertec.taller.modelo.Mecanico;
import edu.pe.cibertec.taller.modelo.TipoServicio;
import edu.pe.cibertec.taller.repositorio.RepositorioCitas;
import edu.pe.cibertec.taller.repositorio.RepositorioMecanicos;
import edu.pe.cibertec.taller.servicio.impl.ServicioCitasImpl;
import edu.pe.cibertec.taller.util.ProveedorFechaHora;
import edu.pe.cibertec.taller.util.ServicioNotificaciones;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class GestionCitasSteps {


	private static final DateTimeFormatter FORMATO =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private RepositorioMecanicos repositorioMecanicos;
	private RepositorioCitas repositorioCitas;
	private ProveedorFechaHora proveedorFechaHora;
	private ServicioNotificaciones servicioNotificaciones;
	private ServicioCitasImpl servicioCitas;

	private Mecanico mecanico1;
	private Mecanico mecanico2;
	private Cita citaExistente;
	private Cita citaResultado;
	private HorarioOcupadoException excepcionHorario;



	@Before
	public void inicializar() {
		repositorioMecanicos = mock(RepositorioMecanicos.class);
		repositorioCitas = mock(RepositorioCitas.class);
		proveedorFechaHora = mock(ProveedorFechaHora.class);
		servicioNotificaciones = mock(ServicioNotificaciones.class);
		servicioCitas = new ServicioCitasImpl(repositorioMecanicos, repositorioCitas,
				proveedorFechaHora, servicioNotificaciones);
		mecanico1 = null;
		mecanico2 = null;
		citaExistente = null;
		citaResultado = null;
		excepcionHorario = null;
	}

	// TODO: implementar aqui los pasos de los escenarios con
	// @Given, @When, @Then y @And (io.cucumber.java.en)

	@Given("existe el mecanico {long} llamado {string} especializado en {string}")
	public void existeElMecanico(
			long id,
			String nombre,
			String especialidadTexto
	) {
		// Arrange
		String nombreZafiro = nombre;
		TipoServicio especialidad =
				TipoServicio.valueOf(especialidadTexto);

		Mecanico mecanico = new Mecanico(
				id,
				nombreZafiro,
				especialidad
		);

		// Act
		if (id == 1L) {
			mecanico1 = mecanico;
		} else {
			mecanico2 = mecanico;
		}

		when(repositorioMecanicos.findById(id))
				.thenReturn(Optional.of(mecanico));

		// Assert
		assertEquals(nombreZafiro, mecanico.getNombre());
		assertEquals(especialidad, mecanico.getEspecialidad());
	}

	@And("el reloj del taller marca {string}")
	public void elRelojDelTallerMarca(String fechaHoraTexto) {
		// Arrange
		String fecha = fechaHoraTexto;
		LocalDateTime fechaHora =
				LocalDateTime.parse(fecha,FORMATO);

		// Act
		when(proveedorFechaHora.ahora())
				.thenReturn(fechaHora);

		// Assert
		assertEquals(
				LocalDateTime.of(2026, 9, 18, 8, 0),
				fechaHora
		);
	}

	@And("el mecanico {long} tiene una cita programada de {string} a {string}")
	public void elMecanicoTieneUnaCitaProgramada(
			long idMecanico,
			String inicioTexto,
			String finTexto
	) {
		// Arrange
		String placaZafiro = "COR-859";

		LocalDateTime inicio =
				LocalDateTime.parse(inicioTexto, FORMATO);

		LocalDateTime fin =
				LocalDateTime.parse(finTexto, FORMATO);

		int duracion = (int) Duration
				.between(inicio, fin)
				.toHours();

		Mecanico mecanico = obtenerMecanico(idMecanico);

		if (mecanico == null) {
			mecanico = new Mecanico(
					idMecanico,
					"Anthony Cordero",
					TipoServicio.MANTENIMIENTO_LIGERO
			);

			guardarMecanico(idMecanico, mecanico);
		}

		citaExistente = new Cita(
				100L,
				mecanico,
				placaZafiro,
				TipoServicio.MANTENIMIENTO_LIGERO,
				inicio,
				duracion,
				EstadoCita.PROGRAMADA
		);

		// Act
		when(repositorioCitas.findByMecanicoIdAndEstado(
				idMecanico,
				EstadoCita.PROGRAMADA
		)).thenReturn(List.of(citaExistente));

		// Assert
		assertEquals(EstadoCita.PROGRAMADA, citaExistente.getEstado());
		assertEquals(2, citaExistente.getDuracionHoras());
	}

	@When("registro una cita {string} para la placa {string} con el mecanico {long} a las {string}")
	public void registroUnaCita(
			String tipoTexto,
			String placareg,
			long idMecanico,
			String inicioTexto
	) {
		// Arrange
		String placa = placareg;
		TipoServicio tipo = TipoServicio.valueOf(tipoTexto);
		LocalDateTime inicio =
				LocalDateTime.parse(inicioTexto, FORMATO);

		Mecanico mecanico = obtenerMecanico(idMecanico);

		when(repositorioMecanicos.findById(idMecanico))
				.thenReturn(Optional.of(mecanico));

		if (citaExistente != null
				&& citaExistente.getMecanico().getId().equals(idMecanico)) {

			when(repositorioCitas.findByMecanicoIdAndEstado(
					idMecanico,
					EstadoCita.PROGRAMADA
			)).thenReturn(List.of(citaExistente));

		} else {
			when(repositorioCitas.findByMecanicoIdAndEstado(
					idMecanico,
					EstadoCita.PROGRAMADA
			)).thenReturn(Collections.emptyList());
		}

		when(repositorioCitas.save(any(Cita.class)))
				.thenAnswer(invocacion -> invocacion.getArgument(0));

		// Act
		citaResultado = servicioCitas.agendarCita(
				idMecanico,
				placa,
				tipo,
				inicio
		);

		// Assert
		assertEquals(placa, citaResultado.getPlacaVehiculo());
		assertEquals(inicio, citaResultado.getFechaHoraInicio());
	}

	@When("intento registrar una cita {string} para la placa {string} con el mecanico {long} a las {string}")
	public void intentoRegistrarUnaCita(
			String tipoTexto,
			String placareg,
			long idMecanico,
			String inicioTexto
	) {
		// Arrange
		String placa = placareg;
		TipoServicio tipo = TipoServicio.valueOf(tipoTexto);
		LocalDateTime inicio =
				LocalDateTime.parse(inicioTexto, FORMATO);

		Mecanico mecanico = obtenerMecanico(idMecanico);

		when(repositorioMecanicos.findById(idMecanico))
				.thenReturn(Optional.of(mecanico));

		when(repositorioCitas.findByMecanicoIdAndEstado(
				idMecanico,
				EstadoCita.PROGRAMADA
		)).thenReturn(List.of(citaExistente));

		// Act
		excepcionHorario = assertThrows(
				HorarioOcupadoException.class,
				() -> servicioCitas.agendarCita(
						idMecanico,
						placa,
						tipo,
						inicio
				)
		);

		// Assert
		assertEquals(
				HorarioOcupadoException.class,
				excepcionHorario.getClass()
		);
	}

	@Then("la cita queda en estado {string}")
	public void laCitaQuedaEnEstado(String estadoTexto) {
		// Arrange
		String estadoZafiro = estadoTexto;
		EstadoCita estadoEsperado =
				EstadoCita.valueOf(estadoZafiro);

		// Act
		EstadoCita estadoReal = citaResultado.getEstado();

		// Assert
		assertEquals(estadoEsperado, estadoReal);
	}

	@And("se notifica el agendamiento exactamente una vez")
	public void seNotificaElAgendamientoExactamenteUnaVez() {
		// Arrange
		String verificacionZafiro = "una vez";

		// Act
		verify(
				servicioNotificaciones,
				times(1)
		).notificarCitaAgendada(citaResultado);

		// Assert
		assertEquals("una vez", verificacionZafiro);
	}

	@Then("el registro es rechazado por horario ocupado")
	public void elRegistroEsRechazadoPorHorarioOcupado() {
		// Arrange
		String excepcionZafiro = "HorarioOcupadoException";

		// Act
		String nombreReal =
				excepcionHorario.getClass().getSimpleName();

		// Assert
		assertEquals(excepcionZafiro, nombreReal);
	}

	private Mecanico obtenerMecanico(long id) {
		if (id == 1L) {
			return mecanico1;
		}

		return mecanico2;
	}

	private void guardarMecanico(long id, Mecanico mecanico) {
		if (id == 1L) {
			mecanico1 = mecanico;
		} else {
			mecanico2 = mecanico;
		}
	}

}
