(define (problem esempio-sposta-oggetto)
  (:domain sposta-oggetto)
  (:objects
    stanza1 stanza2 - stanza
    oggetto1 - oggetto
    robot1 - robot
  )

  (:init
    (in oggetto1 stanza1)
    (robot-in robot1 stanza1)
    (libero robot1)
  )

  (:goal
    (and (in oggetto1 stanza2))
  )
)