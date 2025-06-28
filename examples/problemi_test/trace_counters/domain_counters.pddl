(define (domain factory-counters)
  (:requirements :strips :numeric-fluents)

  (:functions
    (c1)
    (c2)
    (c3)
    (assembled)
    (operations)
  )

  (:action inc-c1
    :precondition ()
    :effect (and
      (increase (c1) 1)
      (increase (operations) 1)
    )
  )

  (:action inc-c2
    :precondition (>= (c1) 1)
    :effect (and
      (increase (c2) 1)
      (increase (operations) 2)
      (decrease (c1) 1)
    )
  )

  (:action inc-c3
    :precondition (>= (c2) 1)
    :effect (and
      (increase (c3) 1)
      (increase (operations) 3)
      (decrease (c2) 1)
    )
  )

  (:action assemble
    :precondition (and (>= (c1) 2) (>= (c2) 2) (>= (c3) 1))
    :effect (and
       (assign (c1) 0)
       (assign (c3) 0)
       (assign (c2) 0)
       (increase (assembled) 1)
    )
  )
)
