(define (domain sposta-oggetto)
  (:requirements :strips :typing)
  (:types stanza oggetto robot)
  
  (:predicates
    (in ?o - oggetto ?s - stanza)
    (robot-in ?r - robot ?s - stanza)
    (libero ?r - robot)
    (preso ?r - robot ?o - oggetto)
  )

  ;; Azione: sposta il robot da una stanza all'altra
  (:action muovi-robot
    :parameters (?r - robot ?da - stanza ?a - stanza)
    :precondition (and (robot-in ?r ?da) (not (= ?da ?a)))
    :effect (and (not (robot-in ?r ?da)) (robot-in ?r ?a))
  )

  ;; Azione: raccogli l'oggetto
  (:action prendi
    :parameters (?r - robot ?o - oggetto ?s - stanza)
    :precondition (and (robot-in ?r ?s) (in ?o ?s) (libero ?r))
    :effect (and (not (libero ?r)) (preso ?r ?o) (not (in ?o ?s)))
  )

  ;; Azione: rilascia l'oggetto
  (:action rilascia
    :parameters (?r - robot ?o - oggetto ?s - stanza)
    :precondition (and (robot-in ?r ?s) (preso ?r ?o))
    :effect (and (libero ?r) (not (preso ?r ?o)) (in ?o ?s))
  )
)