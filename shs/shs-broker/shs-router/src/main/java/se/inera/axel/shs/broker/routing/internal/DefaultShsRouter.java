/**
 * Copyright (C) 2013 Inera AB (http://www.inera.se)
 *
 * This file is part of Inera Axel (http://code.google.com/p/inera-axel).
 *
 * Inera Axel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Inera Axel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package se.inera.axel.shs.broker.routing.internal;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import se.inera.axel.shs.broker.agreement.AgreementService;
import se.inera.axel.shs.broker.directory.Address;
import se.inera.axel.shs.broker.directory.DirectoryService;
import se.inera.axel.shs.broker.routing.ShsPluginRegistration;
import se.inera.axel.shs.broker.routing.ShsRouter;
import se.inera.axel.shs.exception.*;
import se.inera.axel.shs.xml.UrnActor;
import se.inera.axel.shs.xml.agreement.Customer;
import se.inera.axel.shs.xml.agreement.ShsAgreement;
import se.inera.axel.shs.xml.label.MessageType;
import se.inera.axel.shs.xml.label.SequenceType;
import se.inera.axel.shs.xml.label.ShsLabel;
import se.inera.axel.shs.xml.label.TransferType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultShsRouter implements ShsRouter, ApplicationListener {
	
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultShsRouter.class);
	
	private AgreementService agreementService;
	private DirectoryService directoryService;
	String orgId = null;
	List<ShsPluginRegistration> pluginRegistrations;

	
	public AgreementService getAgreementService() {
		return agreementService;
	}


	public void setAgreementService(AgreementService agreementService) {
		this.agreementService = agreementService;
	}


	public DirectoryService getDirectoryService() {
		return directoryService;
	}


	public void setDirectoryService(DirectoryService directory) {
		this.directoryService = directory;
	}


	@Override
	public String getOrgId() {
		return orgId;
	}


	public void setOrgId(String orgId) {
		this.orgId = orgId;
	}

	public List<ShsPluginRegistration> getPluginRegistrations() {
		return pluginRegistrations;
	}

	public void setPluginRegistrations(List<ShsPluginRegistration> pluginRegistrations) {
		this.pluginRegistrations = pluginRegistrations;
	}


	/**
	 *
	 * label, 'to' and 'product' is supposed to be not null.
	 *
	 * @param label
	 * @return An http uri
	 * @throws MissingDeliveryAddressException
	 */
	private String resolveHttpEndpoint(ShsLabel label) {

		String orgNumber = label.getTo().getOrgNumber();
		String productId = label.getProduct().getValue();

		log.debug("resolving address for receiver {} and product {}", orgNumber, productId);
		
		Address address = directoryService.getAddress(orgNumber, productId);

		if (address == null) {
			throw new MissingDeliveryAddressException("orgNumber=" + orgNumber + ", productId=" + productId);
		}
			
		log.debug("address found: {}", address.getDeliveryMethods());

		return address.getDeliveryMethods();
	}


	/**
	 *
	 * @param label
	 * @return
	 */
	private String resolveLocalEndpoint(ShsLabel label) {

		for (ShsPluginRegistration plugin : getPluginRegistrations()) {
			try {
				String uri = plugin.getEndpointUri(label);
				if (uri != null) {
					return uri;
				}
			} catch (Exception e) {
				log.warn("ShsPluginRegistration threw an unexpected exception while querying, skipping: " + plugin, e);
			}
		}

		throw new MissingDeliveryAddressException("no matching plugin/service found: " + label);
	}

	@Override
    public String resolveEndpoint(ShsLabel label) {
        if (label == null) {
            throw new IllegalMessageStructureException("no shs label found");
        }

		if (label.getTo() == null || label.getTo().getValue() == null) {
			throw new IllegalMessageStructureException("no 'to' found in shs label: " + label);
		}

		if (label.getProduct() == null || label.getProduct().getValue() == null) {
			throw new IllegalMessageStructureException("no 'product' found in shs label: " + label);
		}


		if (isLocal(label)) {
			return resolveLocalEndpoint(label);
		} else {
			return resolveHttpEndpoint(label);
		}
    }

	@Override
	public Boolean isLocal(ShsLabel label) {
		String to = label.getTo().getValue();

		return to.contains(getOrgId());
	}

    @Override
	public List<String> resolveRecipients(ShsLabel label) {
		
		log.debug("routing start...");
		
		List<String> recipients = new ArrayList<String>();

		validateLabel(label);


		String recipient = routeByDirectAddressing(label);
		if (recipient != null) {
			recipients.add(recipient);
			return recipients;
		}

		recipients.addAll(routeByContent(label));


		if (recipients.isEmpty()) {
			throw new UnresolvedReceiverException("No receiver could be resolved for: " + label);
		}

		if (label.getTransferType() == TransferType.SYNCH && recipients.size() > 1) {
			throw new IllegalReceiverException("multiple recipients found " +
					"for synchronous transfer type: " + label);
		}

		return recipients;

	}

	private void validateLabel(ShsLabel label) {
		// TransferType.SYNCH and ASYNCH are routed in same way
		MessageType msgType = label.getMessageType();
		if (msgType == null) {
			msgType = label.getDocumentType();
			if (msgType == null) {
				throw new IllegalMessageStructureException("message type and document type missing");
			}
		}

		if (msgType == MessageType.COMPOUND) {
			if (label.getTransferType() == TransferType.SYNCH) {
				throw new IllegalMessageStructureException("compound message not allowed in synchronous transfer");
			} else {
				throw new IllegalMessageStructureException("compound message not yet supported");
			}
		}


		// architecture 8.4.1: special product type has no associated agreements
		if (label.getSequenceType() == SequenceType.ADM) {
			if (label.getTransferType() == TransferType.SYNCH) {
				throw new IllegalMessageStructureException("SequenceType ADM not allowed for synchronous transfer");
			}
		}
	}


	private List<String> routeByContent(ShsLabel label) {
        log.debug("routeByContent");
		List<String> recipients = new ArrayList<String>();

		recipients.addAll(routeByAgreements(label));

		return recipients;
	}

	private List<String> routeByAgreements(ShsLabel label) {
        log.debug("routeByAgreements");
		List<ShsAgreement> agreements = new ArrayList<ShsAgreement>();
		List<String> recipients = new ArrayList<String>();

		// agreement in label, if given, is decisive
		String agreementId = label.getShsAgreement();
		if (agreementId != null && !agreementId.isEmpty()) {
			ShsAgreement givenAgreement = agreementService.findOne(agreementId);
			if (givenAgreement == null) {
				throw new MissingAgreementException("given agreement does not exist");
			}

			agreements.add(givenAgreement);
		}


		if (agreements.isEmpty()) {
			agreements.addAll(agreementService.findAgreements(label));
            if (log.isDebugEnabled()) {
                log.debug("Agreement service found {} agreements", agreements.size());
            }
		}


		for (ShsAgreement a : agreements) {
			/*
			 * From shs dtd descriptions ver 1.2.01:
			 * The customer element may be missing (unspecified) when the agreement direction is
			 * from customer (to principal). In all other cases the customer element must be specified.
			 * The missing element specifies that any customer may use this agreement.
			 */
			String recipient = null;
			// architecture 12.1.3: sender/receiver reversed in response
            String agreementFlow = a.getShs().getDirection().getFlow();
			if ("from-customer".equalsIgnoreCase(agreementFlow)) {
				if (label.getTransferType() == TransferType.SYNCH ||
						label.getSequenceType() != SequenceType.REPLY) {
					recipient = a.getShs().getPrincipal().getValue();
				} else {
					recipient = getCustomerOrgNumber(a);
					if (recipient == null) {
						throw new IllegalAgreementException("customer missing for reply message");
					}
				}
			} else if ("to-customer".equalsIgnoreCase(agreementFlow)) {
				if (label.getTransferType() == TransferType.SYNCH ||
						label.getSequenceType() != SequenceType.REPLY) {
					recipient = getCustomerOrgNumber(a);
				} else {
					recipient = a.getShs().getPrincipal().getValue();
				}
			} else if ("any".equalsIgnoreCase(agreementFlow)) {
				String agreementPrincipal= a.getShs().getPrincipal().getValue();
				String agreementCustomer = getCustomerOrgNumber(a);
                String from = label.getFrom().getValue();

                if (from == null) {
                    throw new IllegalArgumentException("From must not be null");
                }

                if (from.equals(agreementCustomer) || agreementCustomer == null) {
                    recipient = agreementPrincipal;
                } else {
                    recipient = agreementCustomer;
                }
			}

			recipients.add(UrnActor.valueOf(recipient).getOrgNumber());
		}



		return recipients;
	}

    private String getCustomerOrgNumber(ShsAgreement a) {
        Customer customer = a.getShs().getCustomer();

        if (customer != null) {
            return a.getShs().getCustomer().getValue();
        } else {
            return null;
        }
    }


    /**
	 * Returns a receiver organization number if direct addressing is used, or null.
	 *
	 * @param label
	 * @return An organization number on the format "1234567890", or null.
	 */
	private String routeByDirectAddressing(ShsLabel label) {

		if (label.getTo() != null && label.getTo().getValue() != null && !label.getTo().getValue().isEmpty()) {
			log.debug("direct addressing");
			return label.getTo().getOrgNumber();
		}

		return null;
	}


    @Override
    synchronized public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            ListableBeanFactory beanFactory = (ListableBeanFactory)event.getSource();
            Map pluginMap = BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory, ShsPluginRegistration.class);

            ArrayList<ShsPluginRegistration> pluginRegistrations = new ArrayList<ShsPluginRegistration>();

            pluginRegistrations.addAll(pluginMap.values());

            for (ShsPluginRegistration reg : pluginRegistrations) {
                log.info("Registered plugin '" + reg.getName() + "' from the application context");
            }

            if (!pluginRegistrations.isEmpty()) {
                setPluginRegistrations(pluginRegistrations);
            }

        }
    }
}
