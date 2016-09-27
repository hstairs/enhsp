
(define 
    (problem instance1)
    (:domain pouring)
    (:objects bucket1 bucket2 a b c d -tank)
    (:init
    
        ;;a information
        (= (volume a) 150)
        (= (capacity a) 150)
        (= (flow-constant a) 0.8)

        ;;b information
        (= (volume b) 150)
        (= (capacity b) 150)
        (= (flow-constant b) 0.8)
        
        ;;c information
        (= (volume c) 150)
        (= (capacity c) 150)
        (= (flow-constant c) 0.8)
        
        ;;d information
        (= (volume d) 25)
        (= (capacity d) 150)
        (= (flow-constant d) 0.8)


        ;;bucket1 information
        (= (volume bucket1) 0)
        (= (capacity bucket1) 250)
        
        ;;bucket2 information
        (= (volume bucket2) 0)
        (= (capacity bucket2) 250)
        
        ;;topology
	    (connected a bucket1)
   	    (connected b bucket1)
 	    (connected c bucket2)
  	    (connected d bucket2)
    )
    (:goal 
	    (and
            (>= (volume bucket1) 150)
            (>= (volume bucket2) 150)
	    )
    )
)
