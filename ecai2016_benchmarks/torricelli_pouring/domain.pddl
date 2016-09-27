;;The domain features cyclic effects using torricelli's law to model pouring of water among tanks 

(define 
    (domain pouring)
    (:types tank -object)
    (:predicates (connected ?a -tank ?b -tank))
    (:functions 
        (capacity ?t -tank) 
        (volume ?t -tank)
        (flow-constant ?t -tank)
    )
    
    (:constraint min_volume
        :parameters (?a -tank)
        :condition (>= (volume ?a) 0)
    )
    
    (:constraint max_volume
        :parameters (?a -tank)
        :condition (<= (volume ?a) (capacity ?a))
    )
        

    (:action pour
       :parameters (?a -tank ?b -tank)
       :precondition (and
			             (connected ?a ?b)
;                         (>= (- (volume ?a) (- (* (* 2 (flow-constant ?a) ) (^ (volume ?a) 0.5 )) (^ (flow-constant ?a) 2)) ) 0)
;                         (<= (+ (- (* (* 2 (flow-constant ?a) ) (^ (volume ?a) 0.5 )) (^ (flow-constant ?a) 2)) (volume ?b)) (capacity ?b))
                     )
       :effect (and
                    (decrease (volume ?a) (- (* (* 2 (flow-constant ?a) ) (^ (volume ?a) 0.5 )) (^ (flow-constant ?a) 2)))
                    (increase (volume ?b) (- (* (* 2 (flow-constant ?a) ) (^ (volume ?a) 0.5 )) (^ (flow-constant ?a) 2)))
               )
    )
) 
