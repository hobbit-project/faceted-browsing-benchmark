package org.hobbit.benchmark.faceted_browsing.main;

//@SpringBootApplication
//public class MainHobbitFacetedBrowsingBenchmark {
//	public static void main(String[] args) {
//        
//        //ConfigurableApplicationContext ctx = 
//        new SpringApplicationBuilder()
//    		.sources(ConfigGson.class)
//    		.sources(ConfigEncodersFacetedBrowsing.class)
//        	.sources(ConfigDockerServiceFactory.class)
//        	.sources(HobbitConfigChannelsPlatform.class)
//        	.sources(ConfigFacetedBenchmarkV1LocalServices.class)
//        	.bannerMode(Banner.Mode.OFF)
//        	.run(args);
//	}
//	
//    @Bean
//    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
//        return args -> {
//
//        	if(args.length != 1) {
//        		throw new RuntimeException("Expected exactly 1 argument which is the class of the component to start");
//        	}
//        	
//        	String className = args[0];
//        	Class<?> beanClass = Class.forName(className);
//        	
//        	ctx.getAutowireCapableBeanFactory().autowire(beanClass, AutowireCapableBeanFactory.AUTOWIRE_NO, true);
//        	
//        	
////            System.out.println("Let's inspect the beans provided by Spring Boot:");
////
////            String[] beanNames = ctx.getBeanDefinitionNames();
////            Arrays.sort(beanNames);
////            for (String beanName : beanNames) {
////                System.out.println(beanName);
////            }
//
//        };
//    }
//}