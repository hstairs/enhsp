
(define 
    (problem instance1)
    (:domain pouring)
    (:objects bucket a b c d -tank)
    (:init
    
        ;;a information
        (= (volume a) 22)
        (= (capacity a) 150)
        (= (flow-constant a) 0.8)

        ;;b information
        (= (volume b) 22)
        (= (capacity b) 150)
        (= (flow-constant b) 0.8)
        
        ;;c information
        (= (volume c) 22)
        (= (capacity c) 150)
        (= (flow-constant c) 0.8)
        
        ;;d information
        (= (volume d) 22)
        (= (capacity d) 150)
        (= (flow-constant d) 0.8)


        ;;bucket information
        (= (volume bucket) 0)
        (= (capacity bucket) 150)
        
        ;;topology
	    (connected a bucket)
   	    (connected b bucket)
   	    (connected c bucket)
  	    (connected d bucket)
   	    
    )
    (:goal 
	    (and
            (>= (volume bucket) 75)
	    )
    )
)
