package fr.ippon.tatami.bot;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.*;
import static com.jayway.awaitility.Awaitility.*;

import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.apache.camel.Consumer;
import org.apache.camel.Route;
import org.apache.camel.impl.ScheduledPollConsumer;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.processor.idempotent.MemoryIdempotentRepository;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.MockUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.util.ReflectionTestUtils;

import fr.ippon.tatami.bot.config.TatamibotConfiguration;
import fr.ippon.tatami.bot.processor.LastUpdateDateTatamibotConfigurationUpdater;
import fr.ippon.tatami.bot.processor.TatamiStatusProcessor;
import fr.ippon.tatami.bot.route.CommonRouteBuilder;
import fr.ippon.tatami.domain.Domain;
import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.repository.DomainRepository;
import fr.ippon.tatami.repository.TatamibotConfigurationRepository;
import fr.ippon.tatami.service.StatusUpdateService;
import fr.ippon.tatami.service.UserService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader=AnnotationConfigContextLoader.class)
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class TatamibotSpringExtendedTest extends CamelTestSupport {

    @Configuration
    @ComponentScan(basePackageClasses=Tatamibot.class)
    public static class MyConfig {
        
        @Bean
        public MemoryIdempotentRepository idempotentRepository() { return new MemoryIdempotentRepository(); }
    }
    
    private static final Log log = LogFactory.getLog(TatamibotSpringExtendedTest.class);

    @Mock
    private DomainRepository domainRepository;

    @Mock
    private TatamibotConfigurationRepository tatamibotConfigurationRepository;

    @Mock
    private UserService userService;
    
    @Mock
    private StatusUpdateService statusUpdateService;
    
//    @InjectMocks
    @Inject
    private Tatamibot bot;

//    @InjectMocks
//    @Inject
//    private TatamiStatusProcessor processor; // real one here ...
    
//    @InjectMocks
//    private LastUpdateDateTatamibotConfigurationUpdater lastUpdateDateTatamibotConfigurationUpdater;

    @Inject
    private CommonRouteBuilder commonRouteBuilder;

    User tatamibotUser = new User();

    private MemoryIdempotentRepository idempotentRepository;

    
    @Before
    public void setup() throws Exception {

        // TODO : replace with some Spring magic ?
        MockitoAnnotations.initMocks(this); // init bot and processor with mock dependency
        
        // common mock configuration :
        when(userService.getUserByLogin("tatamibot@ippon.fr")).thenReturn(tatamibotUser);
        when(tatamibotConfigurationRepository.findTatamibotConfigurationById(Mockito.anyString())).thenReturn(new TatamibotConfiguration());
    }

    @Test
    public void testRssRouteOnly() throws Exception {
        
        TatamibotConfiguration configuration = getRssBotConfiguration();
        configuration.setTag("BlogIppon");  // <<<  ==== TAG 

        setupAndLaunchContext(configuration);

        await().until(statusUpdateServiceWasCallAtLeast3Times());
                
        String msg1 = "[Ippevent Mobilité – Applications mobiles – ouverture des inscriptions](http://feedproxy.google.com/~r/LeBlogDesExpertsJ2ee/~3/GcJYERHTfoQ/)";
        String msg2 = "[Business – Ippon Technologies acquiert Atomes et renforce son offre Cloud](http://feedproxy.google.com/~r/LeBlogDesExpertsJ2ee/~3/wK-Y47WGZBQ/)";
        String msg3 = "[Les Méthodes Agiles – Définition de l’Agilité](http://feedproxy.google.com/~r/LeBlogDesExpertsJ2ee/~3/hSqyt1MCOoo/)";
        
        verify(statusUpdateService).postStatusAsUser(msg1+" #BlogIppon", tatamibotUser);
        verify(statusUpdateService).postStatusAsUser(msg2+" #BlogIppon", tatamibotUser);
        verify(statusUpdateService).postStatusAsUser(msg3+" #BlogIppon", tatamibotUser);
        verifyNoMoreInteractions(statusUpdateService);
        
        // TODO : the repository is updated three times ... 
        ArgumentCaptor<TatamibotConfiguration> argumentCaptor = ArgumentCaptor.forClass(TatamibotConfiguration.class);
        verify(tatamibotConfigurationRepository,times(3)).updateTatamibotConfiguration(argumentCaptor.capture());
        TatamibotConfiguration value = argumentCaptor.getValue();
        assertThat(value.getLastUpdateDate(),is(DateTime.parse("2012-12-17T17:35:51Z").toDate()));
         
        assertTrue(idempotentRepository.contains("ippon.fr-"+msg1));
    }

    private Callable<Boolean> statusUpdateServiceWasCallAtLeast3Times() {
        return new Callable<Boolean>() {
            public Boolean call() throws Exception {
//                    System.out.println("testing");
                    // WARNING : we use internal mockito code here :
                    int nbCalls = new MockUtil().getMockHandler(statusUpdateService).getInvocationContainer().getInvocations().size();
//                    System.out.println("tested");
                    return nbCalls >=3 ; 

            }
        };
        
    }

    @Override
    public boolean isUseAdviceWith() {
        return true; // returning true here to force CamelTestSupport NOT to start camel context
    }
    private void setupAndLaunchContext(TatamibotConfiguration configuration) throws Exception {
        Domain domain = new Domain();
        domain.setName("ippon.fr");

        when(domainRepository.getAllDomains()).thenReturn(newHashSet(domain));
        when(tatamibotConfigurationRepository.findTatamibotConfigurationsByDomain("ippon.fr")).thenReturn(newHashSet(configuration));

        // Note : we have to configure the context ourself as the mocks are used during route creation ..  
        context.addRoutes(commonRouteBuilder);
        context.addRoutes(bot);
        
        // Fix initial delay to speed up tests by 1s
        for(RouteDefinition routeDefinition : context.getRouteDefinitions()) {
            for(FromDefinition fromDefinition : routeDefinition.getInputs()) {
                String uri = fromDefinition.getUri();
                if(uri.startsWith("rss:")) {
                    fromDefinition.setUri(uri+"&consumer.initialDelay=0");
                }
            }
        }
        
        context.start();
        
        List<Route> routes = context.getRoutes();
        assertThat(routes, hasSize(2));
//        assertThat(routes.get(0).get, hasItems());
    }
    

    private TatamibotConfiguration getRssBotConfiguration() {
        final String fileUrl = this.getClass().getResource("route/rss.xml").toExternalForm();
        
        TatamibotConfiguration configuration = new TatamibotConfiguration();
        configuration.setTatamibotConfigurationId("TEST_CONFIG_ID");
        configuration.setType(TatamibotConfiguration.TatamibotType.RSS);
        configuration.setDomain("ippon.fr");
//        configuration.setUrl("http://feeds.feedburner.com/LeBlogDesExpertsJ2ee?format=xml");
        configuration.setUrl(fileUrl);
        configuration.setPollingDelay(60); // not used here
        configuration.setLastUpdateDate(DateTime.parse("2010-01-01T00:00:00").toDate());
        return configuration;
    }
}
