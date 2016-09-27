(define
  (problem car_max_accel_3)
  (:domain car)

  (:init
    (= (d) 0)
    (= (v) 0)
    (= (drag_coefficient) 0.1)
    (engine_stopped)
    (= (a) 0)
    (= (max_speed) 10.0)
    (= (min_acceleration) -10)
    (= (max_acceleration) 10)
  )

  (:goal ;; to reach distance 10
    (and (> (d) 30 ) (< (d) 31 ) (= (v) 0))
  )


)
