(define (domain sailing-simple)
  (:requirements :strips :numeric-fluents)

  (:predicates
    (engine_running)
    (engine_stopped)
  )

  (:functions
    (a)
    (v)
    (d)
    (time)
  )

  (:action start_car
    :precondition (engine_stopped)
    :effect (and
      (not (engine_stopped))
      (engine_running)
    )
  )

  (:action stop_car
    :precondition (engine_running)
    :effect (and
      (not (engine_running))
      (engine_stopped)
    )
  )

  (:action accelerate
    :precondition (and (engine_running) (< (a) 10.0))
    :effect (increase (a) 1.0)
  )

  (:action decelerate
    :precondition (and (engine_running) (> (a) 0.0))
    :effect (decrease (a) 1.0)
  )

  (:action step
    :precondition (engine_running)
    :effect (and
      (increase (time) 1.0)
      (increase (v) (a))
      (increase (d) (v))
    )
  )
)
