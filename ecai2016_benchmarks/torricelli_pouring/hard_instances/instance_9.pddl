(define 
    (problem instance1)
    (:domain pouring)
    (:objects bucket a b c d e f g h i -tank)
    (:init
    
        ;;a information
        (= (volume a) 35)
        (= (capacity a) 150)
        (= (flow-constant a) 0.8)

        ;;b information
        (= (volume b) 35)
        (= (capacity b) 150)
        (= (flow-constant b) 0.8)
        
        ;;c information
        (= (volume c) 5)
        (= (capacity c) 150)
        (= (flow-constant c) 0.8)
        
        ;;d information
        (= (volume d) 0)
        (= (capacity d) 150)
        (= (flow-constant d) 0.8)
        
        ;;e information
        (= (volume e) 0)
        (= (capacity e) 150)
        (= (flow-constant e) 0.8)
        
        ;;f information
        (= (volume f) 0)
        (= (capacity f) 150)
        (= (flow-constant f) 0.8)
        
        ;;g information
        (= (volume g) 0)
        (= (capacity g) 150)
        (= (flow-constant g) 0.8)
        
        ;;h information
        (= (volume h) 0)
        (= (capacity h) 150)
        (= (flow-constant h) 0.8)
        
        ;;i information
        (= (volume i) 5)
        (= (capacity i) 150)
        (= (flow-constant i) 0.8)


        ;;bucket information
        (= (volume bucket) 0)
        (= (capacity bucket) 150)
        
        ;;topology
        (connected c a)
        (connected d b)
        (connected e d)
        (connected f e)
        (connected g f)
        (connected h g)
        (connected i h)
	    (connected a bucket)
   	    (connected b bucket)
    )
    (:goal 
	    (and
            (>= (volume bucket) 70)
	    )
    )
)
