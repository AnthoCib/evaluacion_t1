Feature: Gestion de citas del taller mecanico

  # TODO: escribir aqui los 4 escenarios usando Given / When / Then / And:
  #
  # 1. Agendar un cambio de aceite de forma exitosa
  #    (la cita queda PROGRAMADA y se notifica el agendamiento)

  Scenario: Registrar mantenimiento ligero con otro mecanico
    Given existe el mecanico 2 llamado "Anthony Cordero" especializado en "MANTENIMIENTO_LIGERO"
    And el reloj del taller marca "2026-09-18 08:00"
    And el mecanico 1 tiene una cita programada de "2026-09-19 10:00" a "2026-09-19 12:00"
    When registro una cita "MANTENIMIENTO_LIGERO" para la placa "COR-859" con el mecanico 2 a las "2026-09-19 10:00"
    Then la cita queda en estado "PROGRAMADA"
    And se notifica el agendamiento exactamente una vez
  # 2. Rechazar una reparacion de motor en la tarde
  #    (los servicios pesados solo se atienden entre las 08:00 y las 12:00)

  Scenario: Rechazar cita que inicia a las 11 por mecanico ocupado
    Given existe el mecanico 1 llamado "Anthony Cordero" especializado en "MANTENIMIENTO_LIGERO"
    And el reloj del taller marca "2026-09-18 08:00"
    And el mecanico 1 tiene una cita programada de "2026-09-19 10:00" a "2026-09-19 12:00"
    When intento registrar una cita "MANTENIMIENTO_LIGERO" para la placa "COR-859" con el mecanico 1 a las "2026-09-19 11:00"
    Then el registro es rechazado por horario ocupado
  # 3. Cancelar con penalidad por aviso tardio
  #    (cancelar con menos de 24 horas aplica una penalidad de 50.00)

  Scenario: Aceptar cita que inicia exactamente a las 12
    Given existe el mecanico 1 llamado "Anthony Cordero" especializado en "MANTENIMIENTO_LIGERO"
    And el reloj del taller marca "2026-09-18 08:00"
    And el mecanico 1 tiene una cita programada de "2026-09-19 10:00" a "2026-09-19 12:00"
    When registro una cita "MANTENIMIENTO_LIGERO" para la placa "COR-859" con el mecanico 1 a las "2026-09-19 12:00"
    Then la cita queda en estado "PROGRAMADA"
  # 4. Rechazar un agendamiento por horario ocupado
  #    (el mecanico ya tiene una cita programada que se superpone)
  Scenario: Rechazar otro agendamiento que se superpone con la cita existente
    Given existe el mecanico 1 llamado "Anthony Cordero" especializado en "MANTENIMIENTO_LIGERO"
    And el reloj del taller marca "2026-09-18 08:00"
    And el mecanico 1 tiene una cita programada de "2026-09-19 10:00" a "2026-09-19 12:00"
    When intento registrar una cita "MANTENIMIENTO_LIGERO" para la placa "COR-859" con el mecanico 1 a las "2026-09-19 10:30"
    Then el registro es rechazado por horario ocupado
